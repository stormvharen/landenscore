# LandenScore (Paper 1.21.1, Java 21)

De hele plugin zit in EEN Java-bestand: src/main/java/nl/landen/LandenScore.java
(plus plugin.yml, dat Minecraft verplicht heeft). De config.yml zit ingebouwd en wordt bij de eerste start gemaakt.

## Een jar maken (dit moet je zelf doen, een jar kan alleen met een Java-compiler)
Optie 1, zonder iets te installeren: zet deze map in een gratis GitHub-repository.
Het bestand .github/workflows/build.yml bouwt dan automatisch de jar.
Ga naar "Actions" -> het laatste item -> download "LandenScore" -> pak uit -> LandenScore.jar in je plugins-map.
(Lukt het uploaden van de map .github niet? Maak het bestand via "Add file -> Create new file" met de naam
.github/workflows/build.yml en plak de inhoud erin.)

Optie 2, op je eigen pc: installeer JDK 21 en Maven, ga in deze map en doe `mvn clean package`.
De jar staat dan in target/LandenScore.jar.

Vereist op je server: Vault + EssentialsX (of de eigen economie). Optioneel: LuckPerms, PlaceholderAPI, TAB, Multiverse-Core, Phoenix Crates.
Zie de commando's en instellingen in config.yml (na de eerste start in plugins/LandenScore/).
