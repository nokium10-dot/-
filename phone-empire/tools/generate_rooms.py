#!/usr/bin/env python3
"""Generate the original high-resolution pixel-art rooms bundled with SOULBOUND."""
from PIL import Image, ImageDraw
import random, os
import numpy as np

OUT="phone-empire/src/main/assets/rooms"
os.makedirs(OUT,exist_ok=True)
W,H=2048,1152
palettes=[
 ((20,23,38),(50,48,66),(105,89,105)),((24,28,38),(58,48,47),(134,94,66)),
 ((17,27,40),(40,55,70),(102,116,139)),((16,35,31),(34,67,49),(74,121,74)),
 ((28,23,43),(59,47,77),(134,110,156)),((28,29,35),(61,61,65),(126,119,105)),
 ((19,25,37),(45,52,66),(104,113,135)),((30,19,30),(67,39,54),(152,94,106))]
for idx,(floor,wall,trim) in enumerate(palettes):
    random.seed(741+idx)
    base=np.zeros((H,W,3),dtype=np.uint8)
    base[:]=wall
    # Wall and floor planes.
    base[180:]=floor
    im=Image.fromarray(base)
    d=ImageDraw.Draw(im)
    d.rectangle((0,0,W,180),fill=wall)
    d.rectangle((24,24,W-24,H-24),outline=trim,width=18)
    d.rectangle((48,48,W-48,160),outline=trim,width=6)
    # Floor tiles and subtle grout.
    for y in range(210,H,96):
        d.line((35,y,W-35,y),fill=tuple(max(0,c-7) for c in floor),width=3)
        offset=0 if (y//96)%2 else 48
        for x in range(35+offset,W-35,96):
            d.line((x,y,x,y+96),fill=tuple(min(255,c+4) for c in floor),width=2)
    # Room-specific large architectural silhouettes.
    if idx==0:
        d.rectangle((270,330,760,690),fill=(76,65,87),outline=trim,width=18);d.rectangle((300,360,730,470),fill=(188,175,177))
        d.rectangle((1350,250,1540,770),fill=(93,75,67));d.ellipse((1300,180,1590,340),fill=(224,183,91))
    elif idx==1:
        for x in (260,1250): d.rectangle((x,320,x+430,490),fill=(113,78,52),outline=trim,width=12)
        for x in (300,1350): d.rectangle((x,650,x+180,850),fill=(129,88,51),outline=trim,width=10)
    elif idx==2:
        for x in (160,1420):
            for j,col in enumerate(((151,83,72),(80,119,96),(89,96,145),(184,143,75),(117,84,132))):
                d.rectangle((x,260+j*105,x+430,335+j*105),fill=col,outline=(24,25,35),width=5)
    elif idx==3:
        for x,y in ((270,350),(1450,330),(900,250)):
            d.rectangle((x-22,y+100,x+22,y+350),fill=(92,62,45))
            d.ellipse((x-150,y-90,x+150,y+190),fill=(48,105,71),outline=(87,142,92),width=10)
    elif idx in (4,6):
        for x in (340,1500):
            d.rectangle((x,250,x+100,790),fill=(111,115,132),outline=trim,width=12)
            d.rectangle((x-25,240,x+125,285),fill=trim);d.rectangle((x-25,760,x+125,805),fill=trim)
    elif idx==5:
        d.rectangle((250,300,760,480),fill=(106,78,60),outline=trim,width=12)
        d.rectangle((1270,570,1540,850),fill=(118,84,52),outline=trim,width=12)
        for x in range(130,1900,250): d.line((x,190,x,240),fill=trim,width=8)
    else:
        for x in (300,1500):
            d.ellipse((x-130,240,x+130,500),outline=(224,183,91),width=18)
            d.ellipse((x-85,285,x+85,455),outline=(224,183,91),width=8)
    # Exit arches on each side and top/bottom.
    d.rectangle((W//2-115,20,W//2+115,180),fill=(10,11,20),outline=trim,width=10)
    d.rectangle((0,H//2-100,48,H//2+100),fill=(10,11,20))
    d.rectangle((W-48,H//2-100,W,H//2+100),fill=(10,11,20))
    d.rectangle((W//2-115,H-180,W//2+115,H),fill=(10,11,20),outline=trim,width=10)
    # Fine, restrained pixel grain makes each room feel textured rather than flat.
    arr=np.asarray(im).astype(np.int16)
    rng=np.random.default_rng(2000+idx)
    noise=rng.integers(-3,4,size=(H,W,1),dtype=np.int16)
    arr=np.clip(arr+noise,0,255).astype(np.uint8)
    Image.fromarray(arr).save(f"{OUT}/room{idx}.png",optimize=False,compress_level=1)
print("Generated 8 original room backgrounds.")
