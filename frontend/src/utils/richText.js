// Used for previews and display; the backend also sanitizes descriptions on save.
export function cleanDescription(html) {
  const doc = new DOMParser().parseFromString(html || '', 'text/html');
  const allowed = new Set('p br div strong b em i u s ul ol li h1 h2 h3 h4 blockquote pre code hr img video source a table tbody tr td th'.split(' '));
  for (const node of [...doc.body.querySelectorAll('*')].reverse()) {
    if (!allowed.has(node.localName)) { node.remove(); continue; }
    for (const attr of [...node.attributes]) {
      const safeUrl = ['src','href','poster'].includes(attr.name) && /^(https?:\/\/|\/(?!\/))/.test(attr.value);
      if (!safeUrl && !(node.localName === 'source' && attr.name === 'type')) node.removeAttribute(attr.name);
    }
    if (node.localName === 'video') node.setAttribute('controls', '');
  }
  return doc.body.innerHTML;
}
