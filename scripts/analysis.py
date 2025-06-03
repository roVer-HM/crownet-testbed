import pandas as pd
import matplotlib.pyplot as plt

# CSV-Datei einlesen
df = pd.read_csv("beacon_throughput_per_second.csv")

# Zeitstempel-Spalte in datetime umwandeln
df["timestamp"] = pd.to_datetime(df["timestamp"])

# BMAX (Grenze) definieren
BMAX = 500  # Beispielwert, kann angepasst werden

# Plot vorbereiten
plt.figure(figsize=(12, 6))

# Durchsatz über Zeit plotten
plt.plot(df["timestamp"], df["bytes_per_sec"], marker="o", label="Gemessener Durchsatz (Bytes/s)")

# BMAX-Grenze einzeichnen
plt.axhline(BMAX, color="red", linestyle="--", label=f"BMAX = {BMAX} Bytes/s")

# Überschreitungen einfärben
plt.fill_between(df["timestamp"], df["bytes_per_sec"], BMAX,
                 where=(df["bytes_per_sec"] > BMAX),
                 interpolate=True, color='red', alpha=0.3, label="Überschreitung")

# Achsenbeschriftungen und Legende
plt.xlabel("Zeit")
plt.ylabel("Durchsatz (Bytes/s)")
plt.title("Gesamtdurchsatz über Zeit")
plt.legend()
plt.grid(True)
plt.xticks(rotation=45)
plt.tight_layout()

# Plot anzeigen
plt.show()