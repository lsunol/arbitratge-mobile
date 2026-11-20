// Pruebas unitarias: base64url canónico, JSON restringido, normalización, claves.
// Uso: node web/verificador/test/unit.mjs
import assert from "node:assert/strict";
import { encodeBase64Url, decodeBase64UrlStrict } from "../lib/base64url.js";
import { parseRestrictedJson } from "../lib/json-restricted.js";
import { normalizeInput, shortCode, verifyToken } from "../lib/arb1.js";
import { buildTrustedKeys } from "../lib/keys.js";

let n = 0;
const t = async (name, fn) => {
  try {
    await fn();
    n++;
  } catch (e) {
    console.error(`FALLA: ${name}\n`, e);
    process.exitCode = 1;
  }
};

// ---- base64url
await t("b64 ida y vuelta", () => {
  for (let len = 0; len < 70; len++) {
    const b = Uint8Array.from({ length: len }, (_, i) => (i * 37 + len) & 255);
    const s = encodeBase64Url(b);
    assert.deepEqual(decodeBase64UrlStrict(s), b);
    assert.equal(Buffer.from(b).toString("base64url"), s);
  }
});
await t("b64 rechaza no canónico", () => {
  assert.equal(decodeBase64UrlStrict("QR"), null); // bits sobrantes != 0 ("QQ" es canónico)
  assert.deepEqual(decodeBase64UrlStrict("QQ"), Uint8Array.of(0x41));
  assert.equal(decodeBase64UrlStrict("QUF"), null); // "QUE" es el canónico de 'AA'... bits sobrantes
  assert.deepEqual(decodeBase64UrlStrict("QUE"), Uint8Array.of(0x41, 0x41));
});
await t("b64 rechaza longitud ≡ 1 mod 4, alfabeto y relleno", () => {
  for (const s of ["A", "AAAAA", "AA=", "AA==", "AB+/", "ab c", "ñ", "A\nA"]) assert.equal(decodeBase64UrlStrict(s), null, s);
  assert.equal(decodeBase64UrlStrict("-_-_")?.length, 3);
  assert.deepEqual(decodeBase64UrlStrict(""), new Uint8Array(0));
});

// ---- JSON restringido
const ok = (s) => parseRestrictedJson(s);
const bad = (s) => assert.equal(parseRestrictedJson(s), null, s);
await t("json: válidos", () => {
  assert.deepEqual([...ok('{"a":"b","c":12}')], [["a", "b"], ["c", 12]]);
  assert.deepEqual([...ok(' \t\r\n{ "a" : 0 ,\n"b":"x" } \n')], [["a", 0], ["b", "x"]]);
  assert.equal(ok("{}").size, 0);
  assert.equal(ok('{"n":123456789}').get("n"), 123456789);
  assert.equal(ok('{"s":"\\u00e0\\n\\"\\\\\\/\\b\\f\\r\\t"}').get("s"), "à\n\"\\/\b\f\r\t");
  assert.equal(ok('{"s":"\\ud83d\\ude00"}').get("s"), "😀");
  assert.equal(ok('{"s":"😀 l·l"}').get("s"), "😀 l·l");
  assert.equal(ok('{"__proto__":"x"}').get("__proto__"), "x");
});
await t("json: no objeto / mal formado", () => {
  for (const s of ["", "[]", '"a"', "1", "null", "{", "}", '{"a":"b"', '{"a":"b",}', '{,}', '{"a"}', '{"a":}', '{"a" "b"}', '{a:"b"}',
    "{'a':'b'}", '{"a":"b"} x', '{"a":"b"}{}', '{"a":"b"}\u00a0', '\ufeff{"a":"b"}', '{"a":"b"\u000b}']) bad(s);
});
await t("json: tipos no admitidos", () => {
  for (const v of ["true", "false", "null", "[]", "[1]", "{}", '{"x":1}', "1.0", "1.5", "1e3", "1E3", "-1", "-0", "+1", "01", "00", "0x1",
    "1234567890", "1_0", ".5", "5.", "NaN", "Infinity"]) bad(`{"a":${v}}`);
});
await t("json: control sin escapar, escapes inválidos", () => {
  bad('{"a":"x\ty"}');
  bad('{"a":"x\ny"}');
  bad('{"a":"x\u0000y"}');
  bad('{"a":"\\x41"}');
  bad('{"a":"\\u12"}');
  bad('{"a":"\\u12G4"}');
  bad('{"a":"\\'); // escape sin cerrar
  bad('{"a":"abc');
  bad('{"a\u0001":"b"}');
});
await t("json: sustitutos aislados", () => {
  bad('{"a":"\\ud800"}');
  bad('{"a":"\\udc00"}');
  bad('{"a":"\\ud800x"}');
  bad('{"a":"\\ud800\\u0041"}');
  bad('{"a":"\\ud800\\ud800"}');
  bad('{"a":"\\udc00\\ud800"}');
  bad('{"\\ud800":"a"}');
  bad('{"a":"\ud800"}'); // literal aislado
  bad('{"a":"\udc00"}');
});
await t("json: duplicados tras resolver escapes", () => {
  bad('{"nom":"a","nom":"b"}');
  bad('{"nom":"a","n\\u006fm":"b"}');
  bad('{"n\\u006fm":"a","n\\u006Fm":"b"}');
  bad('{"a":1,"a":1}');
  assert.equal(ok('{"nom":"a","Nom":"b"}').size, 2);
});
await t("json: máximo 16 miembros", () => {
  const mk = (k) => "{" + Array.from({ length: k }, (_, i) => `"k${i}":${i}`).join(",") + "}";
  assert.equal(ok(mk(16)).size, 16);
  bad(mk(17));
});

// ---- normalización
await t("normalización §3.7.7", () => {
  assert.equal(normalizeInput(" \tAR\nB1\r.\u00a0x\u200by\ufeff"), "ARB1.xy");
  assert.equal(normalizeInput(""), null);
  assert.equal(normalizeInput(" \n\t\u00a0\u200b\ufeff"), null);
  assert.equal(normalizeInput("a".repeat(2048)).length, 2048);
  assert.equal(normalizeInput("a".repeat(2049)), null);
  assert.equal(normalizeInput(" ".repeat(2049) + "a"), null); // el límite es de la entrada bruta
  assert.equal(normalizeInput("arb1.A-B"), "arb1.A-B"); // ni mayúsculas ni guiones
  assert.equal(normalizeInput("A\u2028B"), "A\u2028B"); // nada más que la lista
});
await t("código corto", () => assert.equal(shortCode("90717b65-8827-45f7-8c85-038cff09cff6"), "9071-7B65"));

// ---- verificación: entradas especiales sin claves
const none = new Map();
await t("prefijo / URL / versión", async () => {
  const code = async (s) => (await verifyToken(s, none)).code;
  assert.equal(await code("HTTP://x"), "IS_URL");
  assert.equal(await code("  hTtPs://x"), "IS_URL");
  assert.equal(await code("httpſ://x"), "BAD_FORMAT"); // sin plegado Unicode
  assert.equal(await code("ARB2.x"), "UNKNOWN_VERSION");
  assert.equal(await code("ARB999.x"), "UNKNOWN_VERSION");
  assert.equal(await code("ARB1000.x"), "BAD_FORMAT");
  assert.equal(await code("ARB0.x"), "BAD_FORMAT");
  assert.equal(await code("ARB01.x"), "BAD_FORMAT");
  assert.equal(await code("arb1.x"), "BAD_FORMAT");
  assert.equal(await code("ARB1"), "BAD_FORMAT");
  assert.equal(await code(undefined), "BAD_FORMAT");
});

// ---- claves
await t("claves: descarta incoherentes y repetidos", async () => {
  const good = {
    kid: "test",
    alg: "ES256",
    spki: "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEpRY4bm511peBPv5faFlGdQKs7xBl3aesN9-fPqKAaygzPlF9kobC3OHEhaTc1GxZFl2G7olvX8BwCCAkCBy9pw",
    sha256_spki: "ada963a9547c4ed37503d9ff50b903cef256dbd38df7710ee52c9c2bdc05e552",
    status: "active",
    valid_from: "2026-01-01",
  };
  const r = await buildTrustedKeys([good]);
  assert.equal(r.keys.size, 1);
  for (const mut of [
    { kid: "Test" }, { kid: "" }, { kid: "a".repeat(17) }, { alg: "ES384" }, { status: "revoked" },
    { sha256_spki: "00".repeat(32) }, { sha256_spki: good.sha256_spki.toUpperCase() },
    { spki: good.spki + "=" }, { spki: "AAAA" }, { spki: good.spki.slice(0, -2) },
  ]) {
    const rr = await buildTrustedKeys([{ ...good, ...mut }]);
    assert.equal(rr.keys.size, 0, JSON.stringify(mut));
    assert.equal(rr.discarded.length, 1);
  }
  assert.equal((await buildTrustedKeys([good, good])).keys.size, 0); // repetido: ninguno
  assert.equal((await buildTrustedKeys([good], ["otro"])).keys.size, 0); // kid != fichero
  assert.equal((await buildTrustedKeys([good], ["test"])).keys.size, 1);
  assert.equal((await buildTrustedKeys([null, "x", []])).keys.size, 0);
});

if (!process.exitCode) console.log(`unit: ${n} pruebas OK`);
