#!/usr/bin/env node
const fs = require('fs');
const path = require('path');
const crypto = require('crypto');

const filePath = process.argv[2];

if (!filePath) {
  console.log('\nUsage: node extract-evidence.js <path_to_downloaded_payload.json>\n');
  process.exit(1);
}

if (!fs.existsSync(filePath)) {
  console.error(`Error: File "${filePath}" not found.`);
  process.exit(1);
}

try {
  const content = fs.readFileSync(filePath, 'utf8');
  const raw = JSON.parse(content);
  const doc = raw.data || raw;

  const encryptedDataBase64 = doc.encryptedData;
  const aesKeyBase64 = doc.clientKeyWrap || (doc.metadata && doc.metadata.clientKeyWrap);

  if (!encryptedDataBase64) {
    console.error('Error: "encryptedData" field missing in payload.');
    process.exit(1);
  }

  if (!aesKeyBase64) {
    console.error('Error: Decryption key ("clientKeyWrap") is missing in payload.');
    process.exit(1);
  }

  const combined = Buffer.from(encryptedDataBase64, 'base64');
  const key = Buffer.from(aesKeyBase64, 'base64');

  // AES-256-GCM layout: [12 bytes IV] + [Ciphertext] + [16 bytes Tag]
  const iv = combined.subarray(0, 12);
  const tag = combined.subarray(combined.length - 16);
  const ciphertext = combined.subarray(12, combined.length - 16);

  const decipher = crypto.createDecipheriv('aes-256-gcm', key, iv);
  decipher.setAuthTag(tag);
  const decrypted = Buffer.concat([decipher.update(ciphertext), decipher.final()]);

  const defaultName = (doc.documentName || 'extracted_document').replace(/[^a-zA-Z0-9_-]/g, '_');
  const targetFileName = doc.metadata?.fileName || `${defaultName}.json`;
  const outPath = path.resolve(path.dirname(filePath), targetFileName);

  fs.writeFileSync(outPath, decrypted);

  console.log('\n========================================');
  console.log('✅ EVIDENCE EXTRACTION SUCCESSFUL');
  console.log('========================================');
  console.log(`Document Type  : ${doc.documentType || 'N/A'}`);
  console.log(`Reference Tag  : ${doc.documentNumber || 'N/A'}`);
  console.log(`Case Number    : ${doc.caseNumber || doc.metadata?.caseNumber || 'N/A'}`);
  console.log(`Integrity Hash : ${doc.dataHash || 'N/A'}`);
  console.log(`Output File    : ${outPath}`);
  console.log('========================================\n');

  try {
    const parsed = JSON.parse(decrypted.toString('utf8'));
    console.log('Decrypted Content Preview:\n', JSON.stringify(parsed, null, 2));
  } catch {
    console.log('Decrypted Text Content Preview:\n', decrypted.toString('utf8').substring(0, 500));
  }
} catch (err) {
  console.error('\n❌ Extraction failed:', err.message);
  process.exit(1);
}
