// Простой confetti: 80 частиц, gravity + drift, без зависимостей.
// Вызов: ProjectHubConfetti.burst() или ProjectHubConfetti.burstAt(x, y).
(function () {
    'use strict';

    var COLORS = ['#0d6efd', '#198754', '#ffc107', '#fd7e14', '#dc3545',
                  '#6f42c1', '#20c997', '#e91e63', '#00bcd4'];

    function makeCanvas() {
        var existing = document.getElementById('confetti-canvas');
        if (existing) return existing;
        var c = document.createElement('canvas');
        c.id = 'confetti-canvas';
        c.style.position = 'fixed';
        c.style.top = '0';
        c.style.left = '0';
        c.style.width = '100vw';
        c.style.height = '100vh';
        c.style.pointerEvents = 'none';
        c.style.zIndex = '9999';
        document.body.appendChild(c);
        return c;
    }

    function rand(min, max) {
        return Math.random() * (max - min) + min;
    }

    function burst(originX, originY) {
        var canvas = makeCanvas();
        canvas.width = window.innerWidth;
        canvas.height = window.innerHeight;
        var ctx = canvas.getContext('2d');

        var ox = originX != null ? originX : canvas.width / 2;
        var oy = originY != null ? originY : canvas.height / 3;

        var particles = [];
        for (var i = 0; i < 90; i++) {
            var angle = rand(0, Math.PI * 2);
            var speed = rand(4, 11);
            particles.push({
                x: ox,
                y: oy,
                vx: Math.cos(angle) * speed,
                vy: Math.sin(angle) * speed - rand(2, 5),
                size: rand(5, 10),
                color: COLORS[Math.floor(Math.random() * COLORS.length)],
                rotation: rand(0, Math.PI * 2),
                vr: rand(-0.25, 0.25),
                alpha: 1
            });
        }

        var start = performance.now();

        function frame(now) {
            ctx.clearRect(0, 0, canvas.width, canvas.height);
            var elapsed = now - start;
            for (var i = 0; i < particles.length; i++) {
                var p = particles[i];
                p.vy += 0.32;        // gravity
                p.vx *= 0.992;       // air drag
                p.x += p.vx;
                p.y += p.vy;
                p.rotation += p.vr;
                p.alpha = Math.max(0, 1 - elapsed / 2200);

                ctx.save();
                ctx.globalAlpha = p.alpha;
                ctx.translate(p.x, p.y);
                ctx.rotate(p.rotation);
                ctx.fillStyle = p.color;
                ctx.fillRect(-p.size / 2, -p.size / 2, p.size, p.size * 0.6);
                ctx.restore();
            }
            if (elapsed < 2500) {
                requestAnimationFrame(frame);
            } else {
                ctx.clearRect(0, 0, canvas.width, canvas.height);
            }
        }
        requestAnimationFrame(frame);
    }

    window.ProjectHubConfetti = { burst: burst };
})();
