// Графики на дашборде: donut «Мои задачи по статусам» + bar «Готово за 7 дней».
// Данные приходят в data-*-атрибутах элементов <canvas>.
(function () {
    'use strict';
    if (typeof Chart === 'undefined') {
        return;
    }
    var theme = document.documentElement.getAttribute('data-bs-theme');
    var textColor = theme === 'dark' ? '#dee2e6' : '#343a40';
    var gridColor = theme === 'dark' ? 'rgba(255,255,255,0.08)' : 'rgba(0,0,0,0.08)';

    Chart.defaults.color = textColor;
    Chart.defaults.borderColor = gridColor;
    Chart.defaults.font.family = "system-ui, -apple-system, 'Segoe UI', sans-serif";

    // Donut по статусам.
    var statusCanvas = document.getElementById('statusChart');
    if (statusCanvas) {
        var todo = Number(statusCanvas.dataset.todo || 0);
        var inProgress = Number(statusCanvas.dataset.inProgress || 0);
        var done = Number(statusCanvas.dataset.done || 0);
        var blocked = Number(statusCanvas.dataset.blocked || 0);
        var hasAny = todo + inProgress + done + blocked > 0;
        new Chart(statusCanvas, {
            type: 'doughnut',
            data: {
                labels: ['К выполнению', 'В работе', 'Готово', 'Заблокировано'],
                datasets: [{
                    data: hasAny ? [todo, inProgress, done, blocked] : [1, 0, 0, 0],
                    backgroundColor: hasAny
                        ? ['#6c757d', '#ffc107', '#198754', '#dc3545']
                        : ['#e9ecef'],
                    borderWidth: 0
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                cutout: '62%',
                plugins: {
                    legend: { position: 'bottom', labels: { boxWidth: 12, padding: 12 } },
                    tooltip: { enabled: hasAny }
                }
            }
        });
    }

    // Bar за 7 дней.
    var doneCanvas = document.getElementById('doneChart');
    if (doneCanvas) {
        var labels = (doneCanvas.dataset.labels || '').split(',').filter(Boolean);
        var values = (doneCanvas.dataset.values || '').split(',').filter(function (s) { return s !== ''; })
            .map(function (v) { return Number(v); });
        new Chart(doneCanvas, {
            type: 'bar',
            data: {
                labels: labels,
                datasets: [{
                    label: 'Готово',
                    data: values,
                    backgroundColor: '#198754',
                    borderRadius: 4
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: { legend: { display: false } },
                scales: {
                    y: { beginAtZero: true, ticks: { precision: 0 } },
                    x: { grid: { display: false } }
                }
            }
        });
    }
})();
