// Snippet para `chrome-devtools-axi eval` (aplanado a una línea): devuelve JSON con scrollWidth, elementos que
// sobresalen por la derecha (over), textos < 16 px y pares con poco contraste.
(() => {
const out = {w: innerWidth, sw: document.documentElement.scrollWidth, over: [], small: [], contrast: []};
const parse = c => { const m = c.match(/[\d.]+/g).map(Number); return {r:m[0],g:m[1],b:m[2],a:m[3]===undefined?1:m[3]}; };
const blend = (f,b) => ({r:f.r*f.a+b.r*(1-f.a),g:f.g*f.a+b.g*(1-f.a),b:f.b*f.a+b.b*(1-f.a),a:1});
const lum = c => { const f=v=>{v/=255;return v<=.03928?v/12.92:Math.pow((v+.055)/1.055,2.4)}; return .2126*f(c.r)+.7152*f(c.g)+.0722*f(c.b); };
const bgOf = el => { let stack=[]; for (let e=el;e;e=e.parentElement){ const c=parse(getComputedStyle(e).backgroundColor); if(c.a>0) stack.push(c); if(c.a===1) break;} let b={r:255,g:255,b:255,a:1}; if (stack.length && stack[stack.length-1].a<1) b = parse(getComputedStyle(document.body).backgroundColor); for(const c of stack.reverse()) b=blend(c,b); return b; };
for (const el of document.querySelectorAll('body *')) {
  const r = el.getBoundingClientRect();
  if (r.width && r.right > innerWidth + 0.5 && !el.closest('.pitch-lines,.sprite')) out.over.push(el.tagName+'.'+el.className+' '+Math.round(r.right));
  const hasText = [...el.childNodes].some(n=>n.nodeType===3 && n.textContent.trim());
  if (hasText && r.width) {
    const cs = getComputedStyle(el); const fs = parseFloat(cs.fontSize);
    if (fs < 16 && !el.closest('.card')) out.small.push(el.tagName+' '+fs);
    let fg = parse(cs.color); const bg = bgOf(el); fg = blend(fg,bg);
    const L1=lum(fg),L2=lum(bg); const cr=(Math.max(L1,L2)+.05)/(Math.min(L1,L2)+.05);
    const large = fs>=24 || (fs>=18.66 && +cs.fontWeight>=700);
    if (cr < (large?3:4.5)) out.contrast.push(el.tagName+'.'+el.className+' '+cr.toFixed(2)+' '+el.textContent.trim().slice(0,25));
    out.min = Math.min(out.min||99, +cr.toFixed(2));
  }
}
return JSON.stringify(out);
})()
