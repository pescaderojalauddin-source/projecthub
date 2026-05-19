// tiny toast helper поверх Bootstrap 5. window.ProjectHubToast.show({...}) или .success/.error/.info
// использует контейнер #toastContainer (создаётся при первом вызове, если его нет)
(function () {
    'use strict';

    function getContainer() {
        var el = document.getElementById('toastContainer');
        if (!el) {
            el = document.createElement('div');
            el.id = 'toastContainer';
            el.className = 'toast-container position-fixed bottom-0 end-0 p-3';
            el.style.zIndex = '1080';
            document.body.appendChild(el);
        }
        return el;
    }

    function escape(s) {
        return String(s == null ? '' : s)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#39;');
    }

    var ICONS = {
        success: 'bi-check-circle-fill',
        error:   'bi-x-octagon-fill',
        warning: 'bi-exclamation-triangle-fill',
        info:    'bi-info-circle-fill'
    };
    var BG = {
        success: 'text-bg-success',
        error:   'text-bg-danger',
        warning: 'text-bg-warning',
        info:    'text-bg-primary'
    };

    function show(opts) {
        opts = opts || {};
        var variant = opts.variant || 'info';
        if (variant === 'danger') variant = 'error';
        var delay = opts.delay == null ? 4500 : opts.delay;
        var icon = ICONS[variant] || ICONS.info;
        var bg = BG[variant] || BG.info;

        var toast = document.createElement('div');
        toast.className = 'toast ' + bg + ' border-0 shadow';
        toast.setAttribute('role', 'alert');
        toast.setAttribute('aria-live', 'assertive');
        toast.setAttribute('aria-atomic', 'true');
        toast.innerHTML =
            '<div class="d-flex">' +
                '<div class="toast-body d-flex align-items-center gap-2">' +
                    '<i class="bi ' + icon + '"></i>' +
                    '<span>' + escape(opts.message || '') + '</span>' +
                '</div>' +
                '<button type="button" class="btn-close btn-close-white me-2 m-auto" ' +
                    'data-bs-dismiss="toast" aria-label="Закрыть"></button>' +
            '</div>';
        getContainer().appendChild(toast);

        if (typeof bootstrap !== 'undefined' && bootstrap.Toast) {
            var t = bootstrap.Toast.getOrCreateInstance(toast, { delay: delay, autohide: delay > 0 });
            toast.addEventListener('hidden.bs.toast', function () { toast.remove(); });
            t.show();
        } else {
 // fallback: показать как обычный блок, исчезает через delay
            toast.style.opacity = '1';
            toast.classList.add('show');
            if (delay > 0) {
                setTimeout(function () { toast.remove(); }, delay);
            }
        }
        return toast;
    }

    window.ProjectHubToast = {
        show: show,
        success: function (msg, opts) { return show(Object.assign({ message: msg, variant: 'success' }, opts || {})); },
        error:   function (msg, opts) { return show(Object.assign({ message: msg, variant: 'error' },   opts || {})); },
        warning: function (msg, opts) { return show(Object.assign({ message: msg, variant: 'warning' }, opts || {})); },
        info:    function (msg, opts) { return show(Object.assign({ message: msg, variant: 'info' },    opts || {})); }
    };
})();
