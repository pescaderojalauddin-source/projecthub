/*
 * drag-and-drop загрузка файлов в задачу
 * подсвечивает зону, при дропе кладёт файл в input[type=file] и сабмитит форму
 */
(function () {
    const zone = document.getElementById('attachmentDropZone');
    const form = document.getElementById('attachmentForm');
    const input = document.getElementById('attachmentFileInput');
    if (!zone || !form || !input) return;

    ['dragenter', 'dragover'].forEach(evt => {
        zone.addEventListener(evt, (e) => {
            e.preventDefault();
            zone.classList.add('drag-over');
        });
    });
    ['dragleave', 'drop'].forEach(evt => {
        zone.addEventListener(evt, () => zone.classList.remove('drag-over'));
    });

    zone.addEventListener('drop', (e) => {
        e.preventDefault();
        if (e.dataTransfer && e.dataTransfer.files && e.dataTransfer.files.length) {
            input.files = e.dataTransfer.files;
            form.submit();
        }
    });

 // кликом по зоне тоже открываем file picker
    zone.addEventListener('click', () => input.click());
})();
