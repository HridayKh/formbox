/* ============================================================================
   FORMBOX LANDING PAGE — INTERACTIVITY
   Scroll animations, navbar behavior, mobile nav, dropdown persistence
   ============================================================================ */

(function () {
    'use strict';

    // ---------- Navbar scroll effect ----------
    const navbar = document.getElementById('navbar');
    let lastScroll = 0;

    function onScroll() {
        const scrollY = window.scrollY;
        if (scrollY > 40) {
            navbar.classList.add('scrolled');
        } else {
            navbar.classList.remove('scrolled');
        }
        lastScroll = scrollY;
    }

    window.addEventListener('scroll', onScroll, {passive: true});
    onScroll(); // init

    // ---------- Mobile nav toggle ----------
    const mobileToggle = document.getElementById('mobileToggle');
    const mobileNav = document.getElementById('mobileNav');
    let mobileOpen = false;

    if (mobileToggle && mobileNav) {
        mobileToggle.addEventListener('click', function () {
            mobileOpen = !mobileOpen;
            mobileNav.classList.toggle('open', mobileOpen);
            mobileToggle.setAttribute('aria-label', mobileOpen ? 'Close menu' : 'Open menu');
            document.body.style.overflow = mobileOpen ? 'hidden' : '';
        });

        // Close mobile nav when clicking a link
        mobileNav.querySelectorAll('a').forEach(function (link) {
            link.addEventListener('click', function () {
                mobileOpen = false;
                mobileNav.classList.remove('open');
                document.body.style.overflow = '';
            });
        });
    }

    // ---------- Dropdown click-to-toggle on touch ----------
    // On hover-capable devices, CSS :hover handles dropdowns.
    // On touch, we toggle via JS.
    const navItems = document.querySelectorAll('.navbar-links .nav-item');

    navItems.forEach(function (item) {
        const link = item.querySelector('.nav-link');
        if (!link || !item.querySelector('.nav-dropdown')) return;

        link.addEventListener('click', function (e) {
            // Only intercept if this dropdown has a caret (i.e., it's a toggle)
            if (!link.querySelector('.caret')) return;

            e.preventDefault();

            // Close other dropdowns
            navItems.forEach(function (other) {
                if (other !== item) other.classList.remove('active');
            });

            item.classList.toggle('active');
        });
    });

    // Close dropdowns when clicking outside
    document.addEventListener('click', function (e) {
        if (!e.target.closest('.nav-item')) {
            navItems.forEach(function (item) {
                item.classList.remove('active');
            });
        }
    });

    // ---------- Scroll reveal (IntersectionObserver) ----------
    const fadeElements = document.querySelectorAll('.fade-up');

    if ('IntersectionObserver' in window) {
        const observer = new IntersectionObserver(function (entries) {
            entries.forEach(function (entry) {
                if (entry.isIntersecting) {
                    entry.target.classList.add('visible');
                    observer.unobserve(entry.target);
                }
            });
        }, {
            threshold: 0.1, rootMargin: '0px 0px -40px 0px'
        });

        fadeElements.forEach(function (el) {
            observer.observe(el);
        });
    } else {
        // Fallback: show everything
        fadeElements.forEach(function (el) {
            el.classList.add('visible');
        });
    }

    // ---------- Smooth scroll for anchor links ----------
    document.querySelectorAll('a[href^="#"]').forEach(function (anchor) {
        anchor.addEventListener('click', function (e) {
            const targetId = this.getAttribute('href');
            if (targetId === '#') return;

            const target = document.querySelector(targetId);
            if (target) {
                e.preventDefault();
                target.scrollIntoView({behavior: 'smooth'});

                // Close mobile nav if open
                if (mobileOpen) {
                    mobileOpen = false;
                    mobileNav.classList.remove('open');
                    document.body.style.overflow = '';
                }
            }
        });
    });

    // ---------- Waitlist form (mock submission) ----------
    const waitlistForm = document.getElementById('waitlistForm');
    const waitlistEmail = document.getElementById('waitlistEmail');

    if (waitlistForm) {
        waitlistForm.addEventListener('submit', function (e) {
            e.preventDefault();

            const email = waitlistEmail.value.trim();
            if (!email || !email.includes('@')) {
                waitlistEmail.style.borderColor = 'var(--error)';
                waitlistEmail.focus();
                return;
            }

            // Mock success
            const btn = waitlistForm.querySelector('.btn');
            const originalText = btn.textContent;
            btn.textContent = '✓ You\'re on the list!';
            btn.style.pointerEvents = 'none';
            btn.style.opacity = '0.7';
            waitlistEmail.disabled = true;
            waitlistEmail.style.borderColor = 'var(--success)';

            setTimeout(function () {
                btn.textContent = originalText;
                btn.style.pointerEvents = '';
                btn.style.opacity = '';
                waitlistEmail.disabled = false;
                waitlistEmail.value = '';
                waitlistEmail.style.borderColor = '';
            }, 3000);
        });

        // Reset border on input
        waitlistEmail.addEventListener('input', function () {
            this.style.borderColor = '';
        });
    }

})();
