// Microcopy en català (04 §7.7, textos literals). Las claves marcadas «(nou)» no estan a §7.7:
// són text de la implementació que 04 hauria d'incorporar o validar.
export const T = {
  // V0
  v_intro: "Comprova una credencial d'àrbitre.",
  v_offline_ready: "Preparat per fer-lo servir sense connexió.",
  v_offline_notready: "Encara no està preparat per fer-lo servir sense connexió.",
  v_offline_state: "Sense connexió",
  v_local: "La credencial es comprova en aquest dispositiu.",
  // Càmera
  cam_rationale: "Necessitem la càmera només per llegir el QR. L'app no desa la imatge.",
  cam_hint: "Apunta la càmera al codi QR de la credencial. Mantén el mòbil quiet.",
  cam_denied:
    "No tenim permís per usar la càmera. Pots activar-lo als ajustos del navegador o triar una de les altres opcions.",
  cam_unsupported: "Aquest dispositiu no permet escanejar des del navegador.",
  cam_busy: "No s'ha pogut obrir la càmera. Tanca altres apps que la facin servir i torna-ho a provar.",
  cam_noqr: "No trobem cap QR. Allunya o acosta el mòbil i evita reflexos.",
  cam_paused: "La càmera s'ha aturat en sortir de l'app. Toca per tornar-la a obrir.", // (nou)
  cam_noqr_alt: "Pots triar una imatge o enganxar el text.", // 04 §5.3
  // Imatge
  img_noqr: "No hem trobat cap QR en aquesta imatge. Prova amb una imatge més nítida o enganxa el text.",
  img_whatsapp: "Si l'has rebut per WhatsApp, demana el PDF original.",
  img_reading: "Llegint la imatge…", // (nou)
  // Text
  clip_failed: "No s'ha pogut llegir el porta-retalls. Enganxa el text a mà.", // (nou)
  paste_empty: "Enganxa o escriu el codi per poder comprovar-lo.", // (nou, 04 §5.5 «aria-describedby»)
  // Resultats
  d_name: "Nom",
  d_result: "Resultat",
  d_exam: "Examen",
  d_date: "Data de l'examen",
  d_score: "Encerts",
  d_time: "Temps",
  d_fails: "Intents fallits",
  d_code: "Codi",
  d_untrusted: "Dades no fiables",
  d_untrusted_btn: "Veure el que s'ha llegit",
  btn_again: "Comprovar una altra credencial",
  btn_retry_v: "Tornar-ho a provar",
  btn_update_pwa: "Buscar actualització",
  update_none: "Ja tens l'última versió del verificador.", // (nou)
  update_found: "Hi ha una versió nova del verificador. Actualitza-la des de l'inici.", // (nou)
  update_offline: "Cal connexió a internet per buscar actualitzacions.", // (nou)
  update_unavailable: "Aquest navegador no permet buscar actualitzacions.", // (nou)
  update_pwa: "Hi ha una versió nova del verificador.",
  keys_discarded: (n) => `Entrades de claus descartades per error: ${n}.`, // (nou)
  // Instal·lació
  install_state_ok: "Preparat: ja pots fer-lo servir sense internet.",
  install_state_wait: "Encara no està tot descarregat. Espera uns segons amb internet.",
  install_state_checking: "Comprovant…", // (nou)
  install_state_unsupported:
    "Aquest navegador (o aquesta adreça) no permet fer-lo servir sense connexió.", // (nou)
  install_state_nosw_offline: "Sense connexió i encara no preparat: obre'l un cop amb internet.", // 04 §5.2
  install_done: "Ja la tens a la pantalla d'inici.", // 04 §5.8
};

/** Vista de cada resultat de ADR 0001 (04 §5.6). */
export const VIEWS = {
  valid: { cls: "r-valid", icon: "✔", word: "VÀLIDA", sub: "El codi no ha estat alterat.", aria: "Vàlida. El codi no ha estat alterat." },
  invalid: {
    cls: "r-invalid",
    icon: "✖",
    word: "NO VÀLIDA",
    sub: "El codi no supera la comprovació.",
    aria: "No vàlida. El codi no supera la comprovació.",
    body: "Pot ser que el document s'hagi modificat, que no sigui d'aquesta organització o que s'hagi llegit malament.",
    todo: ["Torna a escanejar amb més llum.", "Demana el PDF original a la persona.", "Si continua igual, no l'acceptis."],
  },
  unknown: {
    cls: "r-unknown",
    icon: "?",
    word: "VERSIÓ DESCONEGUDA",
    sub: "Aquest verificador no entén aquest tipus de credencial.",
    aria: "Versió desconeguda. Aquest verificador no entén aquest tipus de credencial.",
    body: "Pot ser una credencial d'una versió més nova. Actualitza el verificador. Si no funciona, no l'acceptis fins que es pugui comprovar.",
  },
  format: {
    cls: "r-format",
    icon: "⚠",
    word: "FORMAT INCORRECTE",
    sub: "Això no sembla una credencial d'Arbitratge.",
    aria: "Format incorrecte. Això no sembla una credencial d'Arbitratge.",
    body: "Possibles motius: el QR és d'una altra cosa, o el text està incomplet o tallat.",
  },
};

/** Codi (02 §3.7.8) → vista + motiu específic. `reason` pot ser una funció de {kid, known}: el motiu ha de ser transparent. */
const KEYS = (known) => (known.length ? known.join(", ") : "cap");
export const CODE_VIEW = {
  OK: { view: "valid" },
  BAD_SIGNATURE: {
    view: "invalid",
    reason: ({ kid }) => `El segell no coincideix amb les dades (clau «${kid}»). El document s'ha alterat o s'ha llegit malament.`,
  },
  UNKNOWN_KID: {
    view: "invalid",
    reason: ({ kid, known }) => `La credencial està signada amb la clau «${kid}», que aquest verificador no coneix. Claus conegudes: ${KEYS(known)}.`,
    todo: ["Actualitza el verificador.", "Si continua igual, no l'acceptis."],
  },
  KID_NOT_ACCEPTED: {
    view: "invalid",
    reason: ({ kid, known }) => `La clau «${kid}» és coneguda però ja no s'accepta. Claus acceptades: ${KEYS(known)}.`,
    todo: ["No l'acceptis.", "Demana una credencial emesa amb una versió vigent de l'app."],
  },
  UNKNOWN_VERSION: { view: "unknown" },
  IS_URL: {
    view: "format",
    reason: () => "Aquest QR és un enllaç web, no una credencial. No obris l'enllaç: pot ser fals.",
    replaceBody: true,
  },
  BAD_FORMAT: { view: "format", reason: () => "No és un codi de credencial d'Arbitratge, o està tallat o s'ha llegit malament." },
  BAD_PAYLOAD: { view: "format", reason: () => "El codi té el format correcte però el contingut està malmès o incomplet." },
  BAD_FIELDS: { view: "format", reason: () => "El codi es llegeix però li falta alguna dada o en té una de no permesa." },
};
