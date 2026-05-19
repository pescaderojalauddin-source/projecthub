// простой переключатель тем для Bootstrap 5.3 (data-bs-theme)
// сохраняет выбор в localStorage. Учитывает системную prefers-color-scheme как дефолт
(function () {
    'use strict';

    var STORAGE_KEY = 'projecthub-theme';
    var html = document.documentElement;
    var btn = document.getElementById('themeToggle');
    var icon = document.getElementById('themeToggleIcon');
    if (!btn || !icon) {
        return;
    }

    function syncIcon(theme) {
 // иконка показывает «куда переключимся», а не текущую тему — так понятнее
        if (theme === 'dark') {
            icon.classList.remove('bi-moon-stars');
            icon.classList.add('bi-sun');
        } else {
            icon.classList.remove('bi-sun');
            icon.classList.add('bi-moon-stars');
        }
    }

    syncIcon(html.getAttribute('data-bs-theme') || 'light');

    btn.addEventListener('click', function () {
        var current = html.getAttribute('data-bs-theme') === 'dark' ? 'dark' : 'light';
        var next = current === 'dark' ? 'light' : 'dark';
        html.setAttribute('data-bs-theme', next);
        try {
            localStorage.setItem(STORAGE_KEY, next);
        } catch (e) { /* private mode/quota — ignore */ }
        syncIcon(next);
    });
})();
