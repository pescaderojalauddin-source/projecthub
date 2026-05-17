/*
 * Burndown-график проекта: линия "осталось открыто" + линия "сделано".
 * Считывает данные из data-labels / data-open / data-done на canvas-элементе.
 * Зависит от Chart.js — должен быть подключен раньше этого скрипта.
 */
(function () {
    const canvas = document.getElementById('burndownChart');
    if (!canvas || typeof Chart === 'undefined') return;

    const labels = (canvas.dataset.labels || '').split('|').filter(Boolean);
    const open   = (canvas.dataset.open   || '').split('|').filter(Boolean).map(Number);
    const done   = (canvas.dataset.done   || '').split('|').filter(Boolean).map(Number);

    if (!labels.length) return;

    const dark = document.documentElement.getAttribute('data-bs-theme') === 'dark';
    const grid = dark ? 'rgba(255,255,255,.08)' : 'rgba(0,0,0,.06)';
    const text = dark ? '#cdd2da' : '#495057';

    new Chart(canvas.getContext('2d'), {
        type: 'line',
        data: {
            labels: labels,
            datasets: [
                {
                    label: 'Осталось открыто',
                    data: open,
                    borderColor: '#0d6efd',
                    backgroundColor: 'rgba(13, 110, 253, .15)',
                    fill: true,
                    tension: 0.25,
                    pointRadius: 0,
                    borderWidth: 2
                },
                {
                    label: 'Сделано',
                    data: done,
                    borderColor: '#198754',
                    backgroundColor: 'rgba(25, 135, 84, .12)',
                    fill: false,
                    tension: 0.25,
                    pointRadius: 0,
                    borderWidth: 2,
                    borderDash: [4, 4]
                }
            ]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            interaction: { mode: 'index', intersect: false },
            plugins: {
                legend: { labels: { color: text, font: { size: 11 } } },
                tooltip: { backgroundColor: dark ? '#1e2025' : '#212529' }
            },
            scales: {
                x: { grid: { color: grid }, ticks: { color: text, maxTicksLimit: 8, font: { size: 10 } } },
                y: { grid: { color: grid }, ticks: { color: text, precision: 0, font: { size: 10 } }, beginAtZero: true }
            }
        }
    });
})();
