from pathlib import Path
import math
from PIL import Image, ImageDraw, ImageFont
from docx import Document
from docx.shared import Inches, Pt, RGBColor
from docx.oxml import OxmlElement
from docx.oxml.ns import qn
ROOT=Path.cwd(); OUT=ROOT/'docs'; AS=OUT/'solid-diagrams-v2';AS.mkdir(exist_ok=True)
NAVY='#17354d'; BLUE='#326080'; TEAL='#137b78'; RED='#b44949'
class Canvas:
 def __init__(self,h=800,w=1800):
  self.im=Image.new('RGB',(w,h),'white');self.d=ImageDraw.Draw(self.im)
 def text(self,x,y,s,size=27,color=NAVY):
  self.d.multiline_text((x,y),s,font=ImageFont.truetype('C:/Windows/Fonts/arial.ttf',size),fill=color,spacing=7)
 def box(self,x,y,w,title,ops='',kind='',h=None):
  lines=ops.count('\n')+1 if ops else 0;head=95 if kind else 65;h=h or head+lines*39+20
  self.d.rectangle((x,y,x+w,y+h),fill='#f4f7fa',outline=NAVY,width=3)
  if kind:self.text(x+16,y+10,kind,24)
  size=28
  while self.d.textlength(title,font=ImageFont.truetype('C:/Windows/Fonts/arial.ttf',size))>w-32: size-=1
  self.text(x+16,y+(46 if kind else 16),title,size)
  if ops:self.d.line((x,y+head,x+w,y+head),fill=NAVY,width=2);self.text(x+16,y+head+12,ops,25)
  return (x,y,w,h)
 def line(self,pts,kind='use',label='',at=None,color=BLUE):
  dash=kind in ('use','realize','runtime')
  for p,q in zip(pts,pts[1:]):
   dist=math.dist(p,q)
   if dash:
    for t in range(0,int(dist),22):
     a=t/dist;b=min(t+12,dist)/dist;self.d.line((p[0]+(q[0]-p[0])*a,p[1]+(q[1]-p[1])*a,p[0]+(q[0]-p[0])*b,p[1]+(q[1]-p[1])*b),fill=color,width=3)
   else:self.d.line((p,q),fill=color,width=3)
  p,q=pts[-2:];t=math.atan2(q[1]-p[1],q[0]-p[0]); wings=[(q[0]-24*math.cos(t-a),q[1]-24*math.sin(t-a)) for a in (.48,-.48)]
  if kind=='realize':self.d.polygon([q,*wings],fill='white',outline=color,width=3)
  else:self.d.line([wings[0],q,wings[1]],fill=color,width=3)
  if label:self.text(*(at or ((p[0]+q[0])/2,(p[1]+q[1])/2)),label,24,color)
 def save(self,n):
  p=AS/(n+'.png');self.im.save(p);return p
D=Document();sec=D.sections[0];sec.page_width=Inches(8.27);sec.page_height=Inches(11.69);sec.top_margin=Inches(.67);sec.bottom_margin=Inches(.6);sec.left_margin=sec.right_margin=Inches(.7)
for name in ['Normal','Body Text']:
 s=D.styles[name];s.font.name='Calibri';s.font.size=Pt(11);s.paragraph_format.space_after=Pt(7)
for name in ['Heading 1','Heading 2']:
 D.styles[name].font.color.rgb=RGBColor.from_string('17354D')
D.styles['Heading 1'].font.size=Pt(23);D.styles['Heading 2'].font.size=Pt(13)
sec.header.paragraphs[0].text='TMC  /  SOFTWARE ARCHITECTURE                                       SOLID IMPLEMENTATION'
sec.header.paragraphs[0].runs[0].font.size=Pt(8)
p=sec.footer.paragraphs[0];p.text='TMC Mobile App  •  SOLID implementation report                                       '
fld=OxmlElement('w:fldSimple');fld.set(qn('w:instr'),'PAGE');p._p.append(fld)
def heading(t):D.add_heading(t,1)
def para(t):D.add_paragraph(t)
def sub(t,s):D.add_heading(t,2);para(s)
def fig(p,cap):D.add_picture(str(p),width=Inches(6.85));D.add_paragraph(cap,'Caption')
def code(s):
 p=D.add_paragraph();p.paragraph_format.space_after=Pt(6)
 pr=p._p.get_or_add_pPr();sh=OxmlElement('w:shd');sh.set(qn('w:fill'),'F1F4F7');pr.append(sh)
 for i,line in enumerate(s.splitlines()):
  r=p.add_run(('\n' if i else '')+line);r.font.name='Consolas';r.font.size=Pt(8.5)
def source(s):
 p=D.add_paragraph('Source: '+s);p.paragraph_format.space_after=Pt(6)
 for r in p.runs:r.font.size=Pt(8);r.italic=True
def page():D.add_page_break()
heading('Applying SOLID in the TMC mobile app')
D.add_paragraph('TMC Mobile App • Implementation and design rationale','Subtitle')
para('The TMC Android app allows employees to enroll a device, display a signed QR access pass, and view attendance records offline. A device certificate binds the employee to the device public key. Each pass includes a timestamp and a fresh random value, called a nonce, and is signed with the device private key. This report explains how the application applies the five SOLID principles, which Section 6.2 of the architecture document sets as design guidelines and CO-3 makes a constraint. Each principle below names the classes that apply it and the requirement or decision it serves.')
sub('System structure','The application separates screen presentation, business logic, data storage, and device security. Jetpack Compose renders the screens, while ViewModels hold screen state. Room stores attendance records, DataStore stores session and certificate data, and Android Keystore manages the device key. Hilt assembles these dependencies. The domain contains the business rules and the interfaces required by them.')

c=Canvas(850)
c.box(20,300,420,'Employee','Uses the mobile app',kind='«person»')
c.box(650,300,550,'TMC Mobile App','Enrollment / QR passes\nAttendance / device status',kind='«implemented application»')
c.box(1260,35,520,'TMC Central Server','Authentication / certificates\nAttendance records',kind='«planned system»')
c.box(1260,610,520,'Gate station','Scan and validate QR pass',kind='«planned external system»')
c.line([(440,390),(650,390)],'runtime',label='operates',at=(465,345),color=TEAL)
c.line([(980,300),(980,140),(1260,140)],'runtime',label='HTTPS (planned)',at=(985,90),color=TEAL)
c.line([(980,493),(980,710),(1260,710)],'runtime',label='QR presentation',at=(985,655),color=TEAL)
fig(c.save('00-context'),'Figure 1. System context. Arrows identify the employee interaction and intended communication channels. The mobile app is implemented; production server integration and gate validation are planned. QR presentation is an optical interaction, not a network call.')
para('Employees use the app to obtain a device certificate and present a signed pass at a gate. The certificate allows subsequent pass generation without a network connection. The planned central server handles authentication, certificate issuance, and attendance retrieval. Gate validation is external to the mobile app and is outside the implemented scope.')
page();heading('Mobile app architecture')

c=Canvas(710)
c.box(610,20,570,'Application','Compose / ViewModels / Hilt',kind='«module»')
for x,title,ops in [(20,'Data storage','Room / DataStore'),(620,'Device security','Android Keystore'),(1220,'Fake server','FakeTmcServer, in-process')]:c.box(x,250,550,title,ops,kind='«module»')
c.box(610,470,570,'Domain','',kind='«module»')
for x in (295,895,1495):
 c.line([(895,174),(895,210),(x,210),(x,250)])
 c.line([(x,404),(x,440),(895,440),(895,470)])
c.line([(610,80),(5,80),(5,550),(610,550)])
c.box(1220,510,550,'TMC Central Server','Planned; not yet implemented',kind='«external system»')
fig(c.save('01-modules'),'Figure 2. Gradle modules. Arrows show source dependencies; every adapter module depends on the domain and nothing depends on an adapter except the application. The fake server module stands in for TMC Central Server, which is not yet implemented.')
para('The app runs against FakeTmcServer, an in-process server in the :fake-server module. It implements AuthApi, EnrollmentApi and AttendanceApi, signs certificates with a fixed server key, and fails with ServerUnreachableException when the phone is offline. TMC Central Server is the planned backend; a future HTTP adapter, ServerApiClient, will implement the same three interfaces and replace the fake by changing ServerModule only. Gate validation and turnstile control are outside the mobile-app scope.')
para('The domain module uses the plain Kotlin JVM plugin, not the Android plugin. An Android import in the domain therefore fails to compile. The dependency rule of Section 6.2, that source dependencies point toward the business rules, is enforced by the build rather than by convention.')
sub('Diagram notation','Class boxes contain selected attributes and operations. The interface stereotype identifies a contract. Dashed arrows with open heads indicate dependencies; hollow triangles indicate interface realization and point toward the interface. Green arrows represent runtime communication. Each diagram includes only the members needed for its explanation.')

from docx.enum.section import WD_SECTION_START, WD_ORIENT
land=D.add_section(WD_SECTION_START.NEW_PAGE)
land.orientation=WD_ORIENT.LANDSCAPE
land.page_width=Inches(11.69);land.page_height=Inches(8.27)
heading('Mobile app class overview')
c=Canvas(1320,2400)
for y,h,label in [(0,215,'PRESENTATION'),(220,740,'DOMAIN'),(975,330,'ADAPTERS')]:
 c.d.rectangle((5,y,2390,y+h),outline='#b8c8d3',width=2)
 c.text(20,y+8,label,23)
for x,title in [(40,'SignInViewModel'),(840,'PassViewModel'),(1640,'AttendanceViewModel')]:c.box(x,65,700,title,'Exposes screen state')
c.box(40,285,700,'SignInAndEnroll','+ invoke(employeeId, password): Session\nEnrolls through EnrollDevice')
c.box(840,285,700,'ObservePassState','+ invoke(): Flow<PassState>')
c.box(1640,285,700,'GetMonthlyAttendance','+ invoke(month): MonthlyAttendance')
c.box(840,480,700,'GeneratePass','+ invoke(certificate): Pass')
c.box(1640,480,700,'AttendanceSummarizer','- rules: List<AttendanceRule>\n+ summarize(...): MonthlyAttendance')
c.box(40,720,330,'AuthApi','+ signIn(...)',kind='«interface»')
c.box(410,720,330,'EnrollmentApi','+ issueCertificate(...)',kind='«interface»')
c.box(840,720,700,'PayloadSigner','+ sign(data): ByteArray',kind='«interface»')
c.box(1640,720,700,'AttendanceRepository','+ load(session, month): AttendanceLoad',kind='«interface»')
c.box(40,1070,700,'FakeTmcServer','In-process stand-in for TMC Central Server')
c.box(840,1070,700,'AndroidKeystorePayloadSigner','+ sign(data): ByteArray')
c.box(1640,1070,700,'OfflineFirstAttendanceRepository','Uses AttendanceApi, AttendanceCache,\nand NetworkStatus')
for x in (390,1190,1990):c.line([(x,189),(x,285)])
c.line([(390,448),(390,620),(205,620),(205,720)])
c.line([(390,620),(575,620),(575,720)])
c.line([(1190,409),(1190,480)])
c.line([(1190,604),(1190,720)])
c.line([(1990,409),(1990,480)])
c.line([(2340,345),(2370,345),(2370,795),(2340,795)])
for x in (205,575):c.line([(390,1070),(390,990),(x,990),(x,874)],'realize')
c.line([(1190,1070),(1190,874)],'realize')
c.line([(1990,1070),(1990,874)],'realize')
D.add_picture(str(c.save('00-class-overview')),width=Inches(9.4))
D.paragraphs[-1].alignment=1
D.add_paragraph('Figure 3. Main mobile app classes, grouped by layer. Dashed open arrows indicate dependencies; hollow triangles indicate interface realization. All classes shown are implemented. SignInAndEnroll reaches EnrollmentApi through EnrollDevice, which is omitted here.','Caption')
para('The diagram follows enrollment, pass generation, and attendance. Shared stores and device-management use cases are omitted for readability. Hilt assembles the implementations and supplies their interfaces to the consumers. The following diagrams expand the relationships relevant to each SOLID principle.')
portrait=D.add_section(WD_SECTION_START.NEW_PAGE)
portrait.orientation=WD_ORIENT.PORTRAIT
portrait.page_width=Inches(8.27);portrait.page_height=Inches(11.69)
heading('S: Single responsibility')

para('A class should have one reason to change (Section 6.2). The QR feature serves FR-UQ-05, which requires time-limited codes that refresh automatically, and AD-05, which requires signed codes. Building and signing a pass, polling for a new one, encoding the payload and drawing the screen are separate classes, so a change to the QR format does not touch the timing code.')
c=Canvas(830)
c.box(30,30,670,'PassViewModel','Exposes screen state')
c.box(1030,30,730,'ObservePassState','+ invoke(): Flow<PassState>')
c.box(1030,330,730,'GeneratePass','+ invoke(certificate): Pass')
c.box(30,330,670,'CertificateStore','+ current(): DeviceCertificate?',kind='«interface»')
c.box(1030,630,730,'PassPayloadCodec','+ signedContent(...)\n+ encode(...)')
c.line([(700,90),(1030,90)],label='uses',at=(800,50))
c.line([(1395,154),(1395,330)],label='refreshes',at=(1410,225))
c.line([(1030,110),(870,110),(870,410),(700,410)])
c.line([(1395,454),(1395,630)],label='uses',at=(1410,530))
fig(c.save('02-srp'),'Figure 4. Focused class dependency view for QR generation. Timing and certificate-state handling are separate from constructing and encoding a pass.')
code('operator fun invoke(certificate: DeviceCertificate): Pass {\n    val issuedAt = clock.instant()\n    val nonce = nonces.next()\n    val signature = signer.sign(\n        PassPayloadCodec.signedContent(certificate, issuedAt, nonce))\n    return Pass(\n        payload = PassPayloadCodec.encode(certificate, issuedAt, nonce, signature),\n        issuedAt = issuedAt,\n        refreshAt = issuedAt.plus(refreshInterval),\n    )\n}')
D.add_paragraph('Listing 1. Pass construction in GeneratePass.', 'Caption')
sub('Responsibility allocation','GeneratePass constructs one signed pass and sets its refreshAt time, 15 seconds after issue by default. ObservePassState checks the certificate once a second and asks GeneratePass for a new pass when refreshAt has passed or the certificate has changed. PassPayloadCodec defines the encoded format, and Compose handles presentation. GeneratePass receives Clock, NonceSource and PayloadSigner through its constructor, so tests control time, nonce values and signing.')
sub('Enrollment and start-up','Signing in and renewing a certificate both enroll the device: get the device key, request a certificate, store it. EnrollDevice holds these steps once, and SignInAndEnroll and RenewCertificate both call it, so a change to enrollment touches one class. The check that decides whether the app opens on the pass screen or the sign-in screen is the use case IsDeviceEnrolled; StartViewModel only maps its result to a destination. PassViewModel gets the employee through GetSignedInEmployee instead of reading the session store.')
page();heading('O: Open/Closed')
para('The Open/Closed Principle calls for extending behavior without modifying existing logic. Section 6.2 states that a new rule is a new class implementing a common interface, and NFR-09 requires new rules to be added with limited change. AttendanceSummarizer applies a list of AttendanceRule objects to each record. Adding an HR rule requires a new rule implementation and registration in that list; the summary algorithm stays unchanged.')
c=Canvas(660)
c.box(25,40,700,'AttendanceSummarizer','- rules: List<AttendanceRule>\n+ summarize(...): MonthlyAttendance')
c.box(1030,40,730,'AttendanceRule','+ flags(record): List<DayFlag>',kind='«interface»')
c.box(440,450,620,'MissingExitRule','+ flags(record): List<DayFlag>')
c.box(1140,450,620,'ManualEntryRule','+ flags(record): List<DayFlag>')
c.line([(725,110),(1030,110)],label='uses 0..*',at=(795,65))
c.line([(750,450),(750,300),(1270,300),(1270,194)],'realize')
c.line([(1450,450),(1450,194)],'realize')
fig(c.save('03-ocp'),'Figure 5. Strategy-style extension point. Both concrete rules realize the same interface; the summarizer processes their outputs uniformly.')
code('fun interface AttendanceRule {\n    fun flags(record: AttendanceRecord): List<DayFlag>\n}\n\n// From AttendanceSummarizer.summarize (line-wrapped):\n.map { record -> AttendanceDay(record,\n    rules.flatMap { it.flags(record) }, workedOn(record)) }')
D.add_paragraph('Listing 2. AttendanceRule and the rule application step in AttendanceSummarizer.', 'Caption')
sub('Adding an attendance rule','A late-arrival rule can implement flags(record), return a DayFlag, and be included in the rule list. An extension test adds such a rule without changing the existing MissingExitRule or ManualEntryRule. A DayFlag describes an attendance issue and its severity, allowing the summary to identify records that need review.')
sub('Rule registration','AttendanceSummarizer does not choose its rules. DomainModule, the app composition root, provides the rule list and passes it to the summarizer. Adding a default rule means writing the rule class and adding it to this list; AttendanceSummarizer and the existing rules stay unchanged.')
code('@Provides\nfun attendanceRules(): List<AttendanceRule> =\n    listOf(MissingExitRule(), ManualEntryRule())')
D.add_paragraph('Listing 3. Rule registration in DomainModule.', 'Caption')
page();heading('L: Liskov substitution')
para('Replacing an implementation must preserve the behavior its caller relies on. Section 6.2 asks that a simulated adapter behave like the real one from the caller\'s point of view. GeneratePass accepts any PayloadSigner that produces the expected signature and follows the same error contract. AndroidKeystorePayloadSigner provides device signing, while InMemoryDeviceKeyStore supplies signing for unit tests. Shared checks verify their required behavior.')
c=Canvas(650)
c.box(25,30,650,'GeneratePass','+ invoke(certificate): Pass')
c.box(1020,30,740,'PayloadSigner','+ sign(data: ByteArray): ByteArray',kind='«interface»')
c.box(30,450,810,'AndroidKeystorePayloadSigner','+ sign(data: ByteArray): ByteArray')
c.box(940,450,820,'InMemoryDeviceKeyStore','+ sign(data: ByteArray): ByteArray')
c.line([(675,95),(1020,95)])
c.line([(435,450),(435,300),(1210,300),(1210,184)],'realize')
c.line([(1350,450),(1450,450),(1450,184)],'realize')
fig(c.save('04-lsp'),'Figure 6. LSP substitution view. Production and test signers realize PayloadSigner. InMemoryDeviceKeyStore also implements DeviceKeyStore; that interface is omitted here.')
code('fun signaturesVerifyWithPublicKey() {\n    store.createKey()\n    val data = "pass content".toByteArray()\n    val signature = signer.sign(data)\n    assertTrue(EcdsaP256.verify(\n        assertNotNull(store.publicKey()), data, signature))\n}')
D.add_paragraph('Listing 4. Shared signing contract test.', 'Caption')
sub('Hypothetical violation and correction','Consider a test signer that returns empty bytes or throws an unrelated exception when no key exists. Its method signature matches PayloadSigner, but its behavior breaks the signing contract. The correction is to return a verifiable signature and throw DeviceKeyMissingException when the key is absent. InMemoryDeviceKeyStore follows these requirements. The shared contract also checks key creation, reuse, and deletion.')
para('The same contract checks are applied to the in-memory implementation and the Android implementation. Android checks require an emulator or device. The signing test above creates a key, signs known data, and verifies the signature using the corresponding public key. Hardware backing may differ without changing this signing contract.')
para('The server ports follow the same pattern. ServerApiContract runs against StubServer, the test double used by the domain tests, and against FakeTmcServer, which the app uses. Both must reject wrong passwords and forged tokens, fail with ServerUnreachableException when offline, and issue certificates that verify with the server key. The planned ServerApiClient must pass the same contract before it replaces the fake.')
page();heading('I: Interface segregation')
para('Clients should depend only on the operations they use (Section 6.2). TMC defines AuthApi for sign-in, EnrollmentApi for certificate issuance, and AttendanceApi for attendance retrieval. SignInAndEnroll needs only AuthApi from the server, EnrollDevice needs only EnrollmentApi, and OfflineFirstAttendanceRepository needs only AttendanceApi.')
c=Canvas(910)
c.text(25,5,'HYPOTHETICAL VIOLATION: one broad dependency',27,RED)
c.box(25,65,650,'EnrollDevice','Needs certificate issuance only')
c.box(1020,65,740,'ServerApi','+ signIn(...)\n+ issueCertificate(...)\n+ monthlyAttendance(...)',kind='«interface»')
c.line([(675,130),(1020,130)],color=RED)
c.text(25,355,'IMPLEMENTED DESIGN: narrow contracts, one server class',27,TEAL)
for x,title,ops in [(25,'SignInAndEnroll',''),(625,'EnrollDevice',''),(1225,'OfflineFirstAttendanceRepository','')]:
 c.box(x,410,550,title,ops)
c.box(25,560,550,'AuthApi','+ signIn(...): Session',kind='«interface»')
c.box(625,560,550,'EnrollmentApi','+ issueCertificate(...)',kind='«interface»')
c.box(1225,560,550,'AttendanceApi','+ monthlyAttendance(...)',kind='«interface»')
for x in (300,900,1500):c.line([(x,495),(x,560)])
c.box(625,790,550,'FakeTmcServer')
for x in (300,900,1500):c.line([(900,790),(900,755),(x,755),(x,714)],'realize')
fig(c.save('05-isp'),'Figure 7. ISP before/after comparison. The broad ServerApi is hypothetical. FakeTmcServer implements all three narrow interfaces; each client sees only the one it uses.')
code('interface EnrollmentApi {\n    suspend fun issueCertificate(\n        token: String, devicePublicKey: ByteArray): DeviceCertificate\n}')
D.add_paragraph('Listing 5. Certificate issuance contract.', 'Caption')
para('FakeTmcServer implements all three interfaces, and ServerModule provides the one instance three times, once as each interface. Each client keeps its narrow dependency. The planned ServerApiClient will do the same. GeneratePass follows the same approach: PayloadSigner exposes signing without exposing key creation or deletion, which stay on DeviceKeyStore.')
page();heading('D: Dependency inversion')
para('Business logic should depend on interfaces whose implementations supply the platform details (Section 6.2). AD-08 requires that external systems are reached only through adapters and that other code depends on interfaces defined by TMC. GeneratePass depends on PayloadSigner, an interface owned by the domain. AndroidKeystorePayloadSigner implements that interface in the security module. Its source dependency therefore points toward the domain, although calls at runtime reach the security implementation.')
c=Canvas(860)
c.text(25,5,'HYPOTHETICAL VIOLATION: domain names a platform detail',27,RED)
c.box(25,55,650,'GeneratePass');c.box(1020,55,740,'AndroidKeystorePayloadSigner')
c.line([(675,95),(1020,95)],color=RED)
c.text(25,190,'IMPLEMENTED DESIGN',27,TEAL)
c.box(25,260,650,'GeneratePass','- signer: PayloadSigner')
c.box(1020,260,740,'PayloadSigner','+ sign(data): ByteArray',kind='«interface»')
c.line([(675,320),(1020,320)],label='source dependency',at=(710,275))
c.d.line((10,510,1785,510),fill=RED,width=4);c.text(550,465,'Domain: policy and contracts',25);c.text(550,525,'Security: platform detail',25)
c.box(1020,650,740,'AndroidKeystorePayloadSigner','+ sign(data): ByteArray')
c.line([(1390,650),(1390,414)],'realize',label='source dependency',at=(1410,565))
c.line([(350,384),(350,720),(1020,720)],'runtime',label='runtime call through injected signer',at=(365,680),color=TEAL)
fig(c.save('06-dip'),'Figure 8. DIP boundary and control flow. The adapter’s source dependency points upward to the domain interface, while the runtime call reaches the concrete signer below the boundary.')
code('// SecurityModule: app composition root\n@Provides\n@Singleton\nfun payloadSigner(): PayloadSigner = AndroidKeystorePayloadSigner()')
D.add_paragraph('Listing 6. Provider method for the signing adapter.', 'Caption')
para('GeneratePass receives PayloadSigner through its constructor. SecurityModule provides AndroidKeystorePayloadSigner, and DomainModule passes it to the use case. This keeps Android dependencies outside the pure Kotlin domain. Dependency injection supplies the instance; dependency inversion keeps the contract in the domain and makes the adapter depend on it.')
para('Hilt provider functions create the concrete adapters in the app composition root, where application dependencies are assembled. Business classes receive the interfaces they need through their constructors.')
page();heading('Testing and limitations')
t=D.add_table(rows=1,cols=4);t.style='Light Shading Accent 1'
for c,s in zip(t.rows[0].cells,['Principle','Implementation evidence','Change isolated','Serves']):c.text=s
for row in [('SRP','GeneratePass, ObservePassState, PassPayloadCodec; EnrollDevice, IsDeviceEnrolled','Pass building, polling, serialization; enrollment steps','6.2, FR-UQ-05, AD-05'),('OCP','AttendanceRule; rule list in DomainModule','Adding attendance flags','6.2, NFR-09'),('LSP','DeviceKeyStoreContract, ServerApiContract','Replacing key stores and servers','6.2'),('ISP','AuthApi, EnrollmentApi, AttendanceApi; PayloadSigner','Changing capabilities a client does not use','6.2'),('DIP','Domain-owned ports; plain Kotlin domain module; Hilt composition root','Changing platform and server adapters','6.2, AD-08')]:
 for cell,s in zip(t.add_row().cells,row):cell.text=s
sub('Test coverage','Unit tests cover pass generation, attendance rules, cache fallback, use cases, screen state, and server contracts. Instrumented tests exercise Android Keystore and network detection on an emulator or device. GitHub Actions runs ./gradlew assembleDebug test on every pull request; the run for the change that introduced EnrollDevice and the composition-root rule list passed.')
para('For pass generation, tests control time and nonce values through injected dependencies. Attendance tests cover the addition of new rules and retrieval from the offline cache. Shared signing contracts check that implementations return verifiable signatures and report a missing key consistently.')
sub('Implementation limitations','Production integration requires TMC Central Server and its HTTP adapter, including error mapping and contract validation. The app uses ECDSA P-256 signatures, so gate validation must support that format. The signing and key-store adapters must use the same key alias to access the same device key. Design issues that remain:')
for item in [
 'AttendanceViewModel decides which months are available. This is a business rule in the presentation layer.',
 'The pass refresh interval is a constant in GeneratePass, and PassViewModel reads it for the countdown ring. AD-13 requires QR validity to be configuration.',
 'DeviceCertificate knows its own signed encoding, so the entity changes when the wire format changes.',
 'PassPayloadCodec.decode and EcdsaP256.verify are used only by tests. Pass verification belongs to the gate station.',
 'Session data and cached attendance are stored without encryption, which AD-11 requires for personal data at rest.',
]:D.add_paragraph(item,'List Bullet')
para('These were found in a review after the main implementation. Three other issues from that review were fixed: enrollment written in two use cases, ViewModels reading stores directly, and rule registration inside AttendanceSummarizer.')
sub('Conclusion','The application separates pass generation from refresh scheduling, supports additional attendance rules through AttendanceRule, and defines focused interfaces for signing and server access. Shared contracts check replacement behavior. Domain-owned interfaces keep the business logic independent of Android adapters and the planned server integration.')
path=OUT/'TMC_SOLID_Report_With_Overview.docx';D.save(path)
print(path)
