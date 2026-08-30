from pathlib import Path
p=Path('app/src/main/java/com/windroid/xp/MainActivity.kt')
s=p.read_text()
s=s.replace('    "user" to "User Accounts.png"\n)', '    "user" to "User Accounts.png",\n    "power" to "Power.png"\n)')
s=s.replace('''                            StartMenuItem("🌐", "Internet") { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com"))) }\n                            StartMenuItem("📧", "E-mail") { }\n                            Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFD6D6D6)))\n''','')
old='''                Text(\n                    "⏻ Turn Off Computer",\n                    color = Color.White,\n                    fontSize = 12.sp,\n                    modifier = Modifier.clickable { powerOpen = true }.padding(vertical = 8.dp)\n                )'''
new='''                Row(\n                    Modifier.clickable { powerOpen = true }.padding(vertical = 6.dp),\n                    verticalAlignment = Alignment.CenterVertically\n                ) {\n                    val powerIcon = remember { xpIcon(context, "power") }\n                    if (powerIcon != null) {\n                        Image(bitmap = powerIcon, contentDescription = "Turn Off Computer", modifier = Modifier.size(24.dp), contentScale = ContentScale.Fit)\n                        Spacer(Modifier.width(5.dp))\n                    }\n                    Text("Turn Off Computer", color = Color.White, fontSize = 12.sp)\n                }'''
if old not in s: raise SystemExit('power anchor missing')
s=s.replace(old,new,1)
p.write_text(s)
