/* Pip's mouth: included by face.c. Pure white, drawn behind the eyes with the same soft anti-aliased, dithered edge.
   A mouth is a band along a centre line: a smile / frown arc, a corner slant, a wave, with a thickness that is a
   round-capped line when shut and swells into an open mouth in the middle. `boxy` squares the ends (grimaces). */

static face_rect_t mouth_rect(const mouth_t *m, int w, int h)
{
    float hw = m->w * 0.5f + 3, hh = m->h * 0.5f + m->thick * 0.5f + fabsf(m->curve) + fabsf(m->tilt) * 0.5f + fabsf(m->wave) + 3;
    float ext = fabsf(m->roll) < 0.01f ? mx(hw, hh) : hw + hh; /* a turned box: its corners reach this far */
    float x1 = m->x - (fabsf(m->roll) < 0.01f ? hw : ext), x2 = m->x + (fabsf(m->roll) < 0.01f ? hw : ext);
    float y1 = m->y - (fabsf(m->roll) < 0.01f ? hh : ext), y2 = m->y + (fabsf(m->roll) < 0.01f ? hh : ext);
    return (face_rect_t){maxi(0, (int)x1), maxi(0, (int)y1), mini(w - 1, (int)x2 + 1), mini(h - 1, (int)y2 + 1)};
}

/* half thickness and centre line of the band at u = -1..1 along it */
static inline void mouth_prof(float u, float hw, float thick, float open, float curve, float tilt, float wave, float wt, float boxy, float *th, float *yc)
{
    float au = fabsf(u), u2 = u * u, u4 = u2 * u2, u8 = u4 * u4;
    float cap = sqrt_fast(mx(1 - u8, 0));          /* round end of the line */
    float ell = sqrt_fast(mx(1 - u2, 0));          /* the opening is an ellipse ... */
    float box = mn(1.0f, mx(1 - au, 0) * 5.0f);   /* ... or a box with sloping ends */
    *th = thick * cap + open * (ell + (box - ell) * boxy);
    float y = curve * (1 - u2) - curve * 0.4f - tilt * u;
    if (wave != 0) y += wave * sin_fast(u * 4.2f + wt) * (1 - u4);
    *yc = y;
    (void)hw;
}

FACE_HOT static void mouth_rows(const face_t *f, uint16_t *buf, int w, int h, int y0, int y1)
{
    const mouth_t *m = &f->mouth;
    face_rect_t r = mouth_rect(m, w, h);
    float cs = cosf(m->roll), sn = sinf(m->roll), hw = mx(m->w * 0.5f, 1), ihw = 1.0f / hw;
    float curve = m->curve, tilt = m->tilt * 0.5f, wave = m->wave, wt = m->wave_t;
    float thick = m->thick * 0.5f, open = m->h * 0.5f, boxy = clamp01(m->boxy), a0 = clamp01(m->alpha);
    uint16_t white = m->color;
    for (int y = maxi(r.y1, y0); y <= r.y2 && y < y1; y++)
        for (int x = r.x1; x <= r.x2; x++) {
            float px = x + 0.5f - m->x, py = y + 0.5f - m->y;
            float lx = px * cs + py * sn, ly = -px * sn + py * cs;
            float u = lx * ihw, au = fabsf(u);
            if (au > 1.02f) continue;
            float th, yc, th2, yc2;
            mouth_prof(u, hw, thick, open, curve, tilt, wave, wt, boxy, &th, &yc);
            mouth_prof(u + (u < 0 ? -1 : 1) * ihw, hw, thick, open, curve, tilt, wave, wt, boxy, &th2, &yc2);
            float sl = fabsf(yc2 - yc) + fabsf(th2 - th);         /* how steep the edge runs here (per px along x) */
            float d = (fabsf(ly - yc) - th) / sqrt_fast(1 + sl * sl); /* distance to the edge, px */
            float cov = clamp01(0.5f - d);
            if (au > 1) cov *= clamp01((1.02f - au) * 50);
            cov *= a0;
            if (cov <= 0) continue;
            buf[y * w + x] = swap_if(pixel(white, 0, 1, 0, cov, swap_if(buf[y * w + x], f->swapped), x, y), f->swapped);
        }
}
