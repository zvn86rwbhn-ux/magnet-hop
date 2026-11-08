"""Build a subset BMFont atlas from OFL Noto Sans KR and original procedural sounds.
Usage: python3 tools/make_assets.py /path/to/NotoSansKR.ttf
The distributable PNG atlas and WAV files are already included; Pillow is only needed to regenerate.
"""
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont
import sys, math, wave, struct
root=Path(__file__).resolve().parents[1]
out=root/'assets'; out.mkdir(exist_ok=True)
source=''.join(p.read_text() for p in (root/'core/src/main/java').rglob('*.java'))
chars=sorted(set(''.join(chr(i) for i in range(32,127))+''.join(c for c in source if ord(c)>127)))
font=ImageFont.truetype(sys.argv[1],32)
font.set_variation_by_axes([500])
cell=46; padding=2; stride=cell+padding*2;cols=32;rows=(len(chars)+cols-1)//cols
im=Image.new('RGBA',(cols*stride,rows*stride),(255,255,255,0));d=ImageDraw.Draw(im)
lines=['info face="Noto Sans KR Subset" size=32 bold=0 italic=0 charset="" unicode=1 stretchH=100 smooth=1 aa=1 padding=0,0,0,0 spacing=0,0',f'common lineHeight=46 base=37 scaleW={im.width} scaleH={im.height} pages=1 packed=0','page id=0 file="ui.png"',f'chars count={len(chars)}']
for i,ch in enumerate(chars):
 x=i%cols*stride+padding;y=i//cols*stride+padding
 # Render each glyph separately with transparent guards: linear filtering must
 # never sample an adjacent glyph (visible as a stray dot above Korean text).
 tile=Image.new('RGBA',(cell,cell),(255,255,255,0))
 ImageDraw.Draw(tile).text((0,0),ch,font=font,fill='white')
 im.paste(tile,(x,y))
 lines.append(f'char id={ord(ch)} x={x} y={y} width={cell} height={cell} xoffset=0 yoffset=0 xadvance={round(font.getlength(ch))} page=0 chnl=15')
im.save(out/'ui.png');(out/'ui.fnt').write_text('\n'.join(lines)+'\n')
for name,freqs in [('attach',[659,988]),('jump',[330,440]),('switch',[523,659,784]),('retry',[220,147])]:
 samples=[];sr=22050
 for f in freqs:
  n=int(sr*.075)
  for i in range(n):
   env=math.sin(math.pi*i/n)**2
   samples.append(int(7000*env*math.sin(2*math.pi*f*i/sr)))
 with wave.open(str(out/(name+'.wav')),'wb') as w:
  w.setnchannels(1);w.setsampwidth(2);w.setframerate(sr);w.writeframes(struct.pack('<'+'h'*len(samples),*samples))
print(f'Built {len(chars)} glyphs and four original sound effects.')
