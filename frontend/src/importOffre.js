// Favori "Ajouter a Postilyn" : un petit script que l'utilisateur glisse dans sa barre de favoris.
// Sur la page d'une offre (Welcome to the Jungle, LinkedIn, Indeed, HelloWork...), il lit l'offre
// et ouvre Postilyn sur /importer avec les informations dans l'URL (apres le #, jamais envoyees au serveur).

const CLE_ATTENTE = 'jt_import'

// Lit d'abord les donnees structurees schema.org JobPosting que publient la plupart des sites d'emploi,
// sinon le texte selectionne, sinon le contenu principal de la page.
const SCRIPT = `(()=>{
const q=s=>document.querySelector(s);
const m=n=>q('meta[property="'+n+'"],meta[name="'+n+'"]')?.content;
const txt=h=>(new DOMParser().parseFromString((h||'').replace(/<li[^>]*>/gi,'\\n- ').replace(/<(br|\\/p|\\/li|\\/ul|\\/h\\d|\\/div)[^>]*>/gi,'\\n'),'text/html').body.textContent||'').replace(/[ \\t]+/g,' ').replace(/\\n\\s*\\n\\s*/g,'\\n\\n').trim();
let j=null;
for(const s of document.querySelectorAll('script[type="application/ld+json"]')){try{const d=JSON.parse(s.textContent);const l=[].concat(d['@graph']||d);j=l.find(x=>x&&[].concat(x['@type']).includes('JobPosting'));if(j)break}catch(e){}}
const org=j&&j.hiringOrganization;
const loc=j&&[].concat(j.jobLocation||[])[0];
const ville=loc&&loc.address&&(loc.address.addressLocality||loc.address.addressRegion);
const sel=String(getSelection()).trim();
let texte=sel||(j?txt(j.description):'')||((q('main')||q('article')||document.body).innerText||'');
const infos=[ville&&'Lieu : '+ville,j&&j.employmentType&&'Contrat : '+[].concat(j.employmentType).join(', ')].filter(Boolean).join('\\n');
const o={poste:(j&&j.title)||m('og:title')||(q('h1')&&q('h1').innerText)||document.title,
entreprise:(typeof org==='string'?org:org&&org.name)||'',
lien:location.href,
texte:((infos?infos+'\\n\\n':'')+texte).trim().slice(0,12000)};
window.open('__ORIGINE__/importer#'+encodeURIComponent(JSON.stringify(o)),'_blank');
})()`

export function codeFavori(origine = window.location.origin) {
  const code = SCRIPT.replace('__ORIGINE__', origine).replace(/\n/g, '')
  return `javascript:${encodeURIComponent(code)}`
}

/** Offre transmise par le favori (dans l'URL), ou mise de cote avant une connexion. */
export function lireOffreImportee(hash) {
  const brut = hash && hash.length > 1 ? hash.slice(1) : sessionStorage.getItem(CLE_ATTENTE)
  if (!brut) return null
  try {
    let o
    try {
      o = JSON.parse(decodeURIComponent(brut))
    } catch {
      // certains navigateurs decodent deja le # de l'URL
      o = JSON.parse(brut)
    }
    return {
      nomEntreprise: String(o.entreprise || '').slice(0, 255),
      poste: String(o.poste || '').slice(0, 255),
      lienOffre: String(o.lien || '').slice(0, 1000),
      texteOffre: String(o.texte || ''),
    }
  } catch {
    return null
  }
}

export const mettreOffreDeCote = (hash) => sessionStorage.setItem(CLE_ATTENTE, hash.slice(1))
export const oublierOffre = () => sessionStorage.removeItem(CLE_ATTENTE)
export const importEnAttente = () => !!sessionStorage.getItem(CLE_ATTENTE)
