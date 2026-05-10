// Глобальная подвязка CSRF-заголовка ко всем htmx-запросам.
// Подключается из fragments/layout.html в head ПОСЛЕ htmx.
(function () {
    'use strict';
    if (typeof htmx === 'undefined') {
        return;
    }
    var csrfTokenMeta = document.querySelector('meta[name="_csrf"]');
    var csrfHeaderMeta = document.querySelector('meta[name="_csrf_header"]');
    var csrfToken = csrfTokenMeta ? csrfTokenMeta.getAttribute('content') : null;
    var csrfHeader = csrfHeaderMeta ? csrfHeaderMeta.getAttribute('content') : null;
    if (!csrfToken || !csrfHeader) {
        return;
    }
    document.body.addEventListener('htmx:configRequest', function (evt) {
        if (!evt.detail.headers[csrfHeader]) {
            evt.detail.headers[csrfHeader] = csrfToken;
        }
    });
})();
