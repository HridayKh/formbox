package gg.jte.generated.ondemand;
import in.hridaykh.formbox.model.dto.CachedForm;
@SuppressWarnings("unchecked")
@javax.annotation.processing.Generated("gg.jte.TemplateEngine")
public final class JteexampleGenerated {
	public static final String JTE_NAME = "example.jte";
	public static final int[] JTE_LINE_INFO = {0,0,2,2,2,2,2,5,5,5,6,6,6,6,6,6,6,6,6,7,7,8,8,8,11,11,11,14,14,14,2,2,2,2};
	public static void render(gg.jte.html.HtmlTemplateOutput jteOutput, gg.jte.html.HtmlInterceptor jteHtmlInterceptor, CachedForm cachedForm) {
		jteOutput.writeContent("\n<head>\n");
		if (cachedForm.name() != null) {
			jteOutput.writeContent("    <meta name=\"description\"");
			var __jte_html_attribute_0 = cachedForm.name();
			if (gg.jte.runtime.TemplateUtils.isAttributeRendered(__jte_html_attribute_0)) {
				jteOutput.writeContent(" content=\"");
				jteOutput.setContext("meta", "content");
				jteOutput.writeUserContent(__jte_html_attribute_0);
				jteOutput.setContext("meta", null);
				jteOutput.writeContent("\"");
			}
			jteOutput.writeContent(">\n");
		}
		jteOutput.writeContent("    <title>");
		jteOutput.setContext("title", null);
		jteOutput.writeUserContent(cachedForm.honeypotName());
		jteOutput.writeContent("</title>\n</head>\n<body>\n<h1>");
		jteOutput.setContext("h1", null);
		jteOutput.writeUserContent(cachedForm.honeypotName());
		jteOutput.writeContent("</h1>\n<p>Welcome to my example page!</p>\n</body>\n");
	}
	public static void renderMap(gg.jte.html.HtmlTemplateOutput jteOutput, gg.jte.html.HtmlInterceptor jteHtmlInterceptor, java.util.Map<String, Object> params) {
		CachedForm cachedForm = (CachedForm)params.get("cachedForm");
		render(jteOutput, jteHtmlInterceptor, cachedForm);
	}
}
