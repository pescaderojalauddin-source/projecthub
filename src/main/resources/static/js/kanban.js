// Drag-drop канбан: SortableJS перетаскивает карточку между колонками,
// HTMX отправляет POST /tasks/{id}/status. Сервер отвечает 204 No Content
// (см. TaskController.changeStatus с заголовком HX-Request).
(function () {
    'use strict';

    var board = document.getElementById('kanban-board');
    if (!board || typeof Sortable === 'undefined' || typeof htmx === 'undefined') {
        return;
    }

    // CSRF: Spring Security защищает POST. Проставляем заголовок на каждый htmx-запрос.
    var csrfTokenMeta = document.querySelector('meta[name="_csrf"]');
    var csrfHeaderMeta = document.querySelector('meta[name="_csrf_header"]');
    var csrfToken = csrfTokenMeta ? csrfTokenMeta.getAttribute('content') : null;
    var csrfHeader = csrfHeaderMeta ? csrfHeaderMeta.getAttribute('content') : null;
    if (csrfToken && csrfHeader) {
        document.body.addEventListener('htmx:configRequest', function (evt) {
            evt.detail.headers[csrfHeader] = csrfToken;
        });
    }

    function refreshCounts() {
        board.querySelectorAll('.kanban-list').forEach(function (list) {
            var status = list.getAttribute('data-status');
            var count = list.querySelectorAll('.kanban-card').length;
            var counter = board.querySelector('[data-count-for="' + status + '"]');
            if (counter) {
                counter.textContent = String(count);
            }
            // Показ/скрытие плейсхолдера «Пусто».
            var placeholder = list.querySelector('.empty-column-placeholder');
            if (placeholder) {
                placeholder.style.display = count === 0 ? '' : 'none';
            }
        });
    }

    var lists = board.querySelectorAll('.kanban-list');
    lists.forEach(function (list) {
        new Sortable(list, {
            group: 'kanban',
            animation: 150,
            ghostClass: 'sortable-ghost',
            dragClass: 'sortable-drag',
            onEnd: function (evt) {
                refreshCounts();
                if (evt.from === evt.to) {
                    return; // переставили внутри той же колонки — статус не менялся
                }
                var card = evt.item;
                var taskId = card.getAttribute('data-task-id');
                var newStatus = evt.to.getAttribute('data-status');
                if (!taskId || !newStatus) {
                    return;
                }

                htmx.ajax('POST', '/tasks/' + encodeURIComponent(taskId) + '/status', {
                    values: { status: newStatus },
                    swap: 'none',
                    target: 'body'
                }).catch(function () {
                    // Откат: вернуть карточку в исходную колонку.
                    if (evt.from && evt.from !== evt.to) {
                        evt.from.insertBefore(card, evt.from.children[evt.oldIndex] || null);
                        refreshCounts();
                    }
                });
            }
        });
    });
})();
