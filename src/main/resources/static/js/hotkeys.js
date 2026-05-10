// Глобальные хоткеи в духе Linear/Notion: / фокусит поиск, g d → дашборд,
// g p → проекты, n → новая задача (на странице проекта), ? показывает cheatsheet.
// Любая комбинация работает только когда фокус НЕ внутри input/textarea/contenteditable.
(function () {
    'use strict';

    var pending = null;          // первая буква комбо «g …»
    var pendingTimer = null;

    function isTyping(target) {
        if (!target) return false;
        var tag = (target.tagName || '').toLowerCase();
        if (tag === 'input' || tag === 'textarea' || tag === 'select') return true;
        if (target.isContentEditable) return true;
        return false;
    }

    function clearPending() {
        pending = null;
        if (pendingTimer) {
            clearTimeout(pendingTimer);
            pendingTimer = null;
        }
    }

    function navigate(url) {
        window.location.href = url;
    }

    function focusSearch() {
        var input = document.querySelector('.navbar-search input[name="q"]');
        if (input) {
            input.focus();
            input.select();
        }
    }

    function openCheatsheet() {
        var modalEl = document.getElementById('hotkeysCheatsheet');
        if (!modalEl || typeof bootstrap === 'undefined') return;
        var modal = bootstrap.Modal.getOrCreateInstance(modalEl);
        modal.show();
    }

    function findNewTaskLink() {
        var anchors = document.querySelectorAll('a[href]');
        for (var i = 0; i < anchors.length; i++) {
            var href = anchors[i].getAttribute('href') || '';
            if (/\/projects\/\d+\/tasks\/new$/.test(href)) {
                return anchors[i];
            }
        }
        return null;
    }

    document.addEventListener('keydown', function (e) {
        if (e.ctrlKey || e.metaKey || e.altKey) return;
        if (isTyping(e.target)) {
            // Esc внутри поиска возвращает фокус на body.
            if (e.key === 'Escape' && e.target && typeof e.target.blur === 'function') {
                e.target.blur();
            }
            return;
        }

        var key = e.key;

        // ? → cheatsheet
        if (key === '?') {
            e.preventDefault();
            openCheatsheet();
            return;
        }

        // / → фокус поиска
        if (key === '/') {
            e.preventDefault();
            focusSearch();
            return;
        }

        // n → новая задача (если кнопка есть на странице)
        if (key === 'n' && !pending) {
            var link = findNewTaskLink();
            if (link) {
                e.preventDefault();
                navigate(link.getAttribute('href'));
            }
            return;
        }

        // Esc закрывает выпадашку поиска.
        if (key === 'Escape') {
            var dropdown = document.getElementById('searchResults');
            if (dropdown) dropdown.innerHTML = '';
            clearPending();
            return;
        }

        // Двухклавишные комбо: g d, g p, g s
        if (pending === 'g') {
            if (key === 'd') {
                e.preventDefault();
                clearPending();
                navigate('/dashboard');
                return;
            }
            if (key === 'p') {
                e.preventDefault();
                clearPending();
                navigate('/projects');
                return;
            }
            if (key === 's') {
                e.preventDefault();
                clearPending();
                navigate('/swagger-ui.html');
                return;
            }
            clearPending();
            return;
        }
        if (key === 'g') {
            pending = 'g';
            pendingTimer = setTimeout(clearPending, 900);
            return;
        }

        // t → переключить тему (если есть кнопка).
        if (key === 't') {
            var btn = document.getElementById('themeToggle');
            if (btn) {
                e.preventDefault();
                btn.click();
            }
        }
    });
})();
