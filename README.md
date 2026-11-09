# iWindoor Backend

Spring Boot 4 + PostgreSQL. Pencere/kapı tasarım uygulamasının sunucusu: kimlik, şirket bazlı katalog,
proje ve tasarım belgesi saklama, kesim listesinden fiyat.

Çizim ve geometri **istemcide** (Dart `windoor_core`) hesaplanır. Sunucu tasarım belgesini doğrular,
saklar ve istemcinin gönderdiği kesim listesini katalog fiyatlarıyla çarpar.

## Çalıştırma

```bash
# PostgreSQL: windoor_db veritabanı, admin / 123456789 (yerel varsayılanlar)
./mvnw spring-boot:run            # Windows: mvnw.cmd spring-boot:run
```

İlk açılışta Flyway çalışır:
- **Boş veritabanı:** V1 (temel şema) ve V2 kurulur.
- **Mevcut veritabanı (Flyway öncesi):** şema V1 olarak işaretlenir (baseline), sadece V2 uygulanır. Veri silinmez.
  Yine de önce yedek al: `pg_dump -U admin windoor_db > yedek.sql`

Şemayı artık Hibernate değiştirmez (`ddl-auto=validate`). Yeni sütun = yeni dosya: `db/migration/V3__....sql`.

Canlı ortam: `SPRING_PROFILES_ACTIVE=prod` ve `DB_URL`, `DB_USER`, `DB_PASSWORD`, `JWT_SECRET` (base64, en az 256 bit) zorunlu.

## Testler

```bash
./mvnw test                                   # birim testler (veritabanı gerekmez)
IWINDOOR_IT=true ./mvnw test                  # + PostgreSQL entegrasyon testleri
```
Entegrasyon testleri `windoor_test` veritabanını kullanır (`IWINDOOR_TEST_DB_URL` ile değiştirilebilir).
Windows PowerShell: `$env:IWINDOOR_IT="true"; .\mvnw.cmd test`

Uç nokta ekleyince API rota sözleşmesini güncelleyin: `.\mvnw.cmd test -Dtest=ApiRoutesTest -Dapi.routes.update=true`
(`docs/api-routes.txt`; `ApiRoutesTest` fark görürse düşer).

## Katalog

Her şirketin kendi kataloğu ve fiyatları var. Şirket kayıt olurken şablon katalog (`company_id IS NULL`,
`DataSeeder`) kopyalanır. Şablondaki ölçü ve fiyatlar **örnektir**, gerçek seri kataloğundan girilmeli.

| Uç | Açıklama |
| --- | --- |
| `GET /api/v1/catalog/snapshot` | Seriler (geometri dahil), profiller, camlar, aksesuarlar. İstemci yerelde tutar. |
| `GET/POST /api/v1/catalog/series`, `PUT /series/{id}` | Seri ve geometrisi (kasa/kanat/kayıt/çıta yüzü, cam payı, kaynak payı...) |
| `GET /api/v1/catalog/series/{id}/bead?glassThicknessMm=24` | Cam kalınlığına uygun çıta |
| `GET/POST /api/v1/catalog/profiles`, `/profiles/filter?type=SASH`, `PUT /profiles/{id}/price?price=` | Profiller |
| `GET/POST /api/v1/catalog/glasses`, `PUT /glasses/{id}/price?price=` | Camlar (`code`, `visualStyle`, `thicknessMm`) |

`series.geometry` alan adları Dart `SeriesGeometry.fromJson` ile birebir aynıdır.

## Pencere ve tasarım belgesi

| Uç | Açıklama |
| --- | --- |
| `POST /api/v1/projects/{id}/windows` | `{name,width,height}` (eski uygulama) veya `+ seriesId, design` |
| `GET /api/v1/windows/{id}` | Pencere + `design`. Eski pencerelerde belge düğüm ağacından türetilir (`legacyDesign: true`). |
| `PUT /api/v1/windows/{id}` | `{name, quantity, seriesId, design, bom, rowVersion}` tüm belge tek seferde |
| `GET /api/v1/windows/{id}/quote` | Kesim listesi güncel fiyatlarla |
| `DELETE /api/v1/windows/{id}` | |

**Çakışma:** `rowVersion` son okunan değer olmalı. Mobil ve masaüstü aynı pencereyi aynı anda kaydederse
ikincisi `409 STALE_VERSION` alır; güncel hâli yükleyip tekrar dener.

**Tasarım belgesi** (Dart `WindowDesign.toJson`):
```json
{"schemaVersion":1,"width":1800,"height":1700,"series":"12","color":"white",
 "root":{"id":"n1","type":"split","dir":"V","sizes":[600,600,600],"children":[
   {"id":"n2","type":"glass","glass":"4+16+4","duplex":[1,1]},
   {"id":"n3","type":"sash","open":"tiltTurnLeft","child":{"id":"n4","type":"glass","glass":"4+16+4"}},
   {"id":"n5","type":"panel","glass":"PANEL-24"}]}}
```
Düğümler: `split(dir V|H, sizes, children 2-12)`, `sash(open, child)`, `slide(children[2])`, `glass|panel(glass, duplex)`.
`open`: `turnLeft, turnRight, tilt, tiltTurnLeft, tiltTurnRight`. `glass` = katalogdaki cam `code`.

**Kesim listesi (bom)**, 1 adet pencere için:
```json
[{"group":"PROFILE","role":"FRAME","length":1806,"qty":2},
 {"group":"BEAD","length":1198,"qty":4,"glassCode":"4+16+4"},
 {"group":"DUPLEX","length":458,"qty":1},
 {"group":"GLASS","glassCode":"4+16+4","w":408,"h":1086,"qty":1}]
```
`role`: `FRAME, SASH, MULLION, SLIDE_SASH, ADAPTER, EXTENSION` → pencerenin serisindeki o tipteki varsayılan profil.
Çıta, camın kalınlığına göre seçilir (`minGlassMm`–`maxGlassMm`). Dubleks aksesuar kodu `DUPLEX`.
Katalogda bulunamayan malzeme 0 TL ile fiyatlanır ve `warnings` listesine yazılır.

## Eski düğüm tabanlı editör

`/api/v1/design/split|update-type|assign-material` mevcut mobil uygulama için duruyor (şirket kontrolü eklendi).
Bir pencere yeni API ile bir kez kaydedildikten sonra `design` esas alınır; eski ağaç güncellenmez.
Mobil uygulama yeni editöre geçince bu uçlar ve `window_nodes` tablosu kaldırılacak.

## Hatalar

Tüm hatalar RFC 9457 biçiminde: `{"status":409,"title":"STALE_VERSION","code":"STALE_VERSION","detail":"...","errors":[...]}`.
Token yok/geçersiz: 401. Başka şirketin kaydı: 403.
