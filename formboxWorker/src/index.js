// ---------------------------------------------------------------------------
// STUB SERVICES — replace these with your real implementations / imports
// ---------------------------------------------------------------------------
const formCacheService = {
	async verifyHmacAndGetForm(formId, request) {
		// TODO: your HMAC verify logic — throw if invalid to trigger 404 path
		throw new Error('not implemented');
	},
};

const submissionService = {
	async rateLimitPassed(formId, rateLimitRpm) { return true; },
	async isContentTypeJson(request) {
		const ct = request.headers.get('content-type') || '';
		return ct.includes('application/json');
	},
	async filesHaveValidMimeTypes(request) { return true; },
	async validateFields(payload, form) { return true; },
	async asyncSaveSubmission(formId, ip, payload, flagged) { /* TODO */ },
};

const polarCacheService = {
	async getCachedSubmissionBalance(tenantId) { return 100; },
	async asyncDecrementCachedSubmissionBalance(tenantId) { /* TODO */ },
};

const TurnstileVerifier = {
	async turnstileFailed(payload, secretKey) { return false; },
};

const formFileService = {
	async uploadFilesAndInitNotifsWebhooks(form, payload, request) { /* TODO */ },
};

const entitlementsCacheService = {
	async getEntitlements(tenantId) { return { redirectUrlsAllowed: true }; },
};

// ---------------------------------------------------------------------------
// helpers
// ---------------------------------------------------------------------------
function getClientIp(request) {
	return request.headers.get('CF-Connecting-IP') ?? '';
}

function renderView(name, status = 200) {
	// TODO: swap for real HTML templates/KV assets if you want rendered pages
	return new Response(JSON.stringify({ view: name }), {
		status,
		headers: { 'Content-Type': 'application/json' },
	});
}

async function parsePayload(request) {
	const ct = request.headers.get('content-type') || '';
	if (ct.includes('application/json')) {
		return await request.clone().json();
	}
	const formData = await request.clone().formData();
	const payload = {};
	for (const [key, value] of formData.entries()) {
		payload[key] = value; // note: File objects will land here for file fields
	}
	return payload;
}

// ---------------------------------------------------------------------------
// core logic — translated from IndexController.submission
// ---------------------------------------------------------------------------
async function submission(formId, payload, request, env, ctx) {
	const startTime = Date.now();
	let stepStart;

	console.debug(`Processing incoming webhook submission request for form ID: ${formId}`);

	// step 1: HMAC verify (replaces DB existence check)
	stepStart = Date.now();
	let form;
	try {
		form = await formCacheService.verifyHmacAndGetForm(formId, request);
		console.debug(`Step 1 (HMAC Verify) took ${Date.now() - stepStart} ms`);
	} catch (e) {
		console.debug(`Step 1 (HMAC Verify - Failed) took ${Date.now() - stepStart} ms`);
		console.warn(`Submission rejected. HMAC verification failed for form ID ${formId}.`);
		return renderView('submit/form-not-found', 404);
	}

	// step 2: per form rate limit (error 429)
	stepStart = Date.now();
	const rateLimitPassed = await submissionService.rateLimitPassed(formId, form.rateLimitRpm);
	console.debug(`Step 2 (Rate Limit check) took ${Date.now() - stepStart} ms`);
	if (!rateLimitPassed) {
		return renderView('submit/rate-limit', 429);
	}

	// step 3: check submissions quota
	stepStart = Date.now();
	const balance = await polarCacheService.getCachedSubmissionBalance(form.tenantId);
	console.debug(`Step 3 (Quota check) took ${Date.now() - stepStart} ms`);
	if (balance <= 0) {
		return renderView('submit/out-of-submissions', 402);
	}

	// step 4: check if content type allowed
	stepStart = Date.now();
	const isContentTypeJson = await submissionService.isContentTypeJson(request);
	console.debug(`Step 4 (Content-Type check) took ${Date.now() - stepStart} ms`);
	if (!form.allowJson && isContentTypeJson) {
		return renderView('submit/json-not-allowed');
	}

	// step 5: check honeypot
	stepStart = Date.now();
	const isHoneypot = !!(payload[form.honeypotName] ?? '').toString().trim();
	if (isHoneypot) {
		ctx.waitUntil(submissionService.asyncSaveSubmission(form.id, getClientIp(request), payload, true));
		console.debug(`Step 5 (Honeypot caught & saved) took ${Date.now() - stepStart} ms`);
		return renderView('submit/thanks');
	}
	console.debug(`Step 5 (Honeypot check passed) took ${Date.now() - stepStart} ms`);

	// step 6: check turnstile
	stepStart = Date.now();
	const turnstileFailed = await TurnstileVerifier.turnstileFailed(payload, form.turnstileSecretKey);
	if (turnstileFailed) {
		ctx.waitUntil(submissionService.asyncSaveSubmission(form.id, getClientIp(request), payload, true));
		console.debug(`Step 6 (Turnstile failed & saved) took ${Date.now() - stepStart} ms`);
		return renderView('submit/thanks');
	}
	console.debug(`Step 6 (Turnstile verification passed) took ${Date.now() - stepStart} ms`);

	// step 7: abort request if files not allowed (error 400)
	stepStart = Date.now();
	if (!form.allowFiles) {
		try {
			const formData = await request.clone().formData();
			const hasFileParts = [...formData.values()].some(v => v instanceof File);
			if (hasFileParts) {
				console.debug(`Step 7 (File check - forbidden) took ${Date.now() - stepStart} ms`);
				return renderView('submit/files-not-allowed', 400);
			}
		} catch (e) {
			console.debug('Suppressed content parse failure context check. Client sent no multi-part payload structure.');
		}
	}
	console.debug(`Step 7 (File check passed) took ${Date.now() - stepStart} ms`);

	// step 8: abort request if invalid mime type on file (error 400)
	stepStart = Date.now();
	const validMime = await submissionService.filesHaveValidMimeTypes(request);
	console.debug(`Step 8 (MIME type check) took ${Date.now() - stepStart} ms`);
	if (!validMime) {
		return renderView('submit/files-not-allowed', 400);
	}

	// step 9: check custom filters and validations (error 400)
	stepStart = Date.now();
	const validFields = await submissionService.validateFields(payload, form);
	console.debug(`Step 9 (Field validations) took ${Date.now() - stepStart} ms`);
	if (!validFields) {
		return renderView('submit/invalid-fields', 400);
	}

	// step 10: save form payload and metadata
	stepStart = Date.now();
	ctx.waitUntil(submissionService.asyncSaveSubmission(form.id, getClientIp(request), payload, false));
	console.debug(`Step 10 (Save payload) took ${Date.now() - stepStart} ms`);

	// step 11: update leftover submission balance
	stepStart = Date.now();
	ctx.waitUntil(polarCacheService.asyncDecrementCachedSubmissionBalance(form.tenantId));
	console.debug(`Step 11 (Decrement quota balance) took ${Date.now() - stepStart} ms`);

	// steps 13 & 14: async upload files/attachments + 3rd party webhooks/notifs
	stepStart = Date.now();
	ctx.waitUntil(formFileService.uploadFilesAndInitNotifsWebhooks(form, payload, request));
	console.debug(`Steps 13 & 14 (Async upload & webhook init) took ${Date.now() - stepStart} ms`);

	console.log(`Successfully processed submission for form ID: ${formId}`);

	// step 12: return 200 ok
	if (isContentTypeJson) {
		return new Response(JSON.stringify({ view: 'submit/json-response' }), {
			status: 200,
			headers: { 'Content-Type': 'application/json' },
		});
	}

	stepStart = Date.now();
	const entitlements = await entitlementsCacheService.getEntitlements(form.tenantId);
	console.debug(`Entitlements check took ${Date.now() - stepStart} ms`);

	if (!form.redirectUrl || form.redirectUrl.trim() === '' || !entitlements.redirectUrlsAllowed) {
		return renderView('submit/thanks');
	}

	return Response.redirect(form.redirectUrl, 302);
}

// ---------------------------------------------------------------------------
// entrypoint / router — THIS is what "triggers" submission()
// ---------------------------------------------------------------------------
export default {
	async fetch(request, env, ctx) {
		const url = new URL(request.url);

		// Route: POST /forms/:formId/submit
		const match = url.pathname.match(/^\/forms\/([0-9a-fA-F-]{36})\/submit$/);

		if (request.method === 'POST' && match) {
			const formId = match[1];
			const payload = await parsePayload(request);
			return submission(formId, payload, request, env, ctx);
		}

		// fallback / health check (your original test route)
		const data = {
			hello: env.HMAC_SIGNING_KEY,
		};
		return Response.json(data);
	},
};