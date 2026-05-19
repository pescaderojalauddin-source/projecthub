/*
 * простая @-меншен-подсказка для textarea с id="text" (форма комментария)
 * список логинов берётся из data-logins (comma-separated) у самого textarea
 *
 * поведение:
 *   - набираешь "@iv" — снизу под кареткой появляется список совпадений (top-6)
 *   - стрелки ↑/↓ выбирают вариант, Enter/Tab вставляет, Esc закрывает
 *   - закрывается при потере фокуса
 *
 * без внешних зависимостей
 */
(function () {
    const textarea = document.getElementById('text');
    if (!textarea) return;
    const logins = (textarea.dataset.logins || '').split(',').map(s => s.trim()).filter(Boolean);
    if (!logins.length) return;

    let dropdown = null;
    let activeIndex = 0;
    let mentionStart = -1;
    let filtered = [];

    function closeDropdown() {
        if (dropdown && dropdown.parentNode) dropdown.parentNode.removeChild(dropdown);
        dropdown = null;
        mentionStart = -1;
        filtered = [];
        activeIndex = 0;
    }

    function insertMention(login) {
        const value = textarea.value;
        const before = value.substring(0, mentionStart);
        const cursor = textarea.selectionStart;
        const after = value.substring(cursor);
        const inserted = '@' + login + ' ';
        textarea.value = before + inserted + after;
        const pos = before.length + inserted.length;
        textarea.setSelectionRange(pos, pos);
        closeDropdown();
        textarea.focus();
    }

    function updateActiveClass() {
        if (!dropdown) return;
        [...dropdown.children].forEach((el, i) => {
            el.classList.toggle('active', i === activeIndex);
        });
    }

    function renderDropdown() {
        if (!filtered.length) {
            closeDropdown();
            return;
        }
        if (!dropdown) {
            dropdown = document.createElement('div');
            dropdown.className = 'mention-dropdown';
            textarea.parentNode.style.position = 'relative';
            textarea.parentNode.appendChild(dropdown);
        }
        dropdown.innerHTML = '';
        filtered.slice(0, 6).forEach((login, i) => {
            const item = document.createElement('div');
            item.className = 'item' + (i === activeIndex ? ' active' : '');
            item.textContent = '@' + login;
            item.addEventListener('mousedown', (ev) => {
                ev.preventDefault();
                insertMention(login);
            });
            dropdown.appendChild(item);
        });
 // позиционируем под textarea
        const rect = textarea.getBoundingClientRect();
        dropdown.style.left = '0';
        dropdown.style.top  = textarea.offsetTop + textarea.offsetHeight + 'px';
    }

    textarea.addEventListener('input', () => {
        const cursor = textarea.selectionStart;
        const value = textarea.value;
 // ищем "@xxx" перед курсором (до пробела/начала)
        let i = cursor - 1;
        while (i >= 0 && /[A-Za-z0-9._-]/.test(value[i])) i--;
        if (i < 0 || value[i] !== '@') {
            closeDropdown();
            return;
        }
        mentionStart = i;
        const query = value.substring(i + 1, cursor).toLowerCase();
        filtered = logins.filter(l => l.toLowerCase().startsWith(query));
        if (!filtered.length) { closeDropdown(); return; }
        activeIndex = 0;
        renderDropdown();
    });

    textarea.addEventListener('keydown', (e) => {
        if (!dropdown || !filtered.length) return;
        if (e.key === 'ArrowDown') {
            e.preventDefault();
            activeIndex = (activeIndex + 1) % Math.min(filtered.length, 6);
            updateActiveClass();
        } else if (e.key === 'ArrowUp') {
            e.preventDefault();
            const count = Math.min(filtered.length, 6);
            activeIndex = (activeIndex - 1 + count) % count;
            updateActiveClass();
        } else if (e.key === 'Enter' || e.key === 'Tab') {
            e.preventDefault();
            insertMention(filtered[activeIndex]);
        } else if (e.key === 'Escape') {
            closeDropdown();
        }
    });

    textarea.addEventListener('blur', () => {
 // даём время mousedown сработать (insertMention)
        setTimeout(closeDropdown, 150);
    });
})();
