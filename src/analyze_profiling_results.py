#!/usr/bin/env python3
"""
HRFC — Análisis de Resultados de Profiling

Este script procesa el CSV generado por ProfileCollector y produce el análisis
detallado requerido por el HRFC.

USO:
    python analyze_profiling_results.py profiling_results.csv
"""

import sys
import csv
from collections import defaultdict

def analyze_profiling_csv(csv_path):
    print("═" * 79)
    print("  HRFC — ANÁLISIS DE PROFILING DE PROYECTILES")
    print("═" * 79)
    print()
    
    # Leer datos del CSV
    frames = []
    with open(csv_path, 'r') as f:
        reader = csv.DictReader(f)
        for row in reader:
            frames.append({
                'frame': int(row['frame']),
                'activeProjectiles': int(row['activeProjectiles']),
                'fps': int(row['fps']),
                'frameTimeMs': float(row['frameTimeMs']),
                'simulationMs': float(row['simulationMs']),
                'collisionMs': float(row['collisionMs']),
                'renderingMs': float(row['renderingMs']),
            })
    
    if not frames:
        print("ERROR: No se encontraron datos en el CSV.")
        return
    
    print(f"Frames recolectados: {len(frames)}")
    print(f"Duración aprox: {len(frames) / 60:.1f} segundos (asumiendo ~60 UPS)")
    print()
    
    # Agrupar por rangos de proyectiles
    ranges = [
        (0, 499, "0-499"),
        (500, 999, "500-999"),
        (1000, 1499, "1000-1499"),
        (1500, 1999, "1500-1999"),
        (2000, 2499, "2000-2499"),
        (2500, 2999, "2500-2999"),
        (3000, 999999, "3000+")
    ]
    
    grouped = defaultdict(list)
    for frame in frames:
        proj = frame['activeProjectiles']
        for min_p, max_p, label in ranges:
            if min_p <= proj <= max_p:
                grouped[label].append(frame)
                break
    
    # Tabla por cantidad de proyectiles
    print("─" * 79)
    print("  TABLA POR CANTIDAD DE PROYECTILES")
    print("─" * 79)
    print()
    print(f"{'Active Projectiles':<20} {'FPS':<8} {'Frame Time':<12} {'Simulation':<12} {'Collision':<12} {'Rendering':<12}")
    print(f"{'':20} {'':8} {'(ms)':<12} {'(ms)':<12} {'(ms)':<12} {'(ms)':<12}")
    print("-" * 79)
    
    for _, _, label in ranges:
        if label not in grouped or not grouped[label]:
            continue
        
        data = grouped[label]
        avg_fps = sum(f['fps'] for f in data) / len(data)
        avg_frame = sum(f['frameTimeMs'] for f in data) / len(data)
        avg_sim = sum(f['simulationMs'] for f in data) / len(data)
        avg_col = sum(f['collisionMs'] for f in data) / len(data)
        avg_ren = sum(f['renderingMs'] for f in data) / len(data)
        
        print(f"{label:<20} {avg_fps:<8.1f} {avg_frame:<12.2f} {avg_sim:<12.2f} {avg_col:<12.2f} {avg_ren:<12.2f}")
    
    print()
    
    # Punto de degradación
    print("─" * 79)
    print("  PUNTO DE DEGRADACIÓN")
    print("─" * 79)
    print()
    
    fps_thresholds = [55, 45, 30, 20, 10, 5]
    for threshold in fps_thresholds:
        for frame in frames:
            if frame['fps'] <= threshold and frame['activeProjectiles'] > 0:
                print(f"FPS ≤ {threshold:2d}: ~{frame['activeProjectiles']:4d} proyectiles activos (frame #{frame['frame']})")
                break
    
    print()
    
    # Worst case
    worst_frame = max(frames, key=lambda f: f['frameTimeMs'])
    max_proj = max(frames, key=lambda f: f['activeProjectiles'])
    
    print("─" * 79)
    print("  WORST CASE")
    print("─" * 79)
    print()
    print(f"Máximo proyectiles activos:  {max_proj['activeProjectiles']}")
    print(f"Peor frame time:             {worst_frame['frameTimeMs']:.2f} ms")
    print(f"  - Simulation:              {worst_frame['simulationMs']:.2f} ms")
    print(f"  - Collision:               {worst_frame['collisionMs']:.2f} ms")
    print(f"  - Rendering:               {worst_frame['renderingMs']:.2f} ms")
    print()
    
    # Análisis de subsistemas
    print("─" * 79)
    print("  DISTRIBUCIÓN DE TIEMPO POR SUBSISTEMA")
    print("─" * 79)
    print()
    
    total_sim = sum(f['simulationMs'] for f in frames)
    total_col = sum(f['collisionMs'] for f in frames)
    total_ren = sum(f['renderingMs'] for f in frames)
    total_all = total_sim + total_col + total_ren
    
    if total_all > 0:
        print(f"Simulation:  {total_sim / total_all * 100:.1f}%")
        print(f"Collision:   {total_col / total_all * 100:.1f}%")
        print(f"Rendering:   {total_ren / total_all * 100:.1f}%")
    print()
    
    # Bottleneck detection
    print("─" * 79)
    print("  CUELLO DE BOTELLA DETECTADO")
    print("─" * 79)
    print()
    
    avg_sim_all = sum(f['simulationMs'] for f in frames) / len(frames)
    avg_col_all = sum(f['collisionMs'] for f in frames) / len(frames)
    avg_ren_all = sum(f['renderingMs'] for f in frames) / len(frames)
    
    subsystems = [
        ('Rendering', avg_ren_all),
        ('Simulation', avg_sim_all),
        ('Collision', avg_col_all),
    ]
    subsystems.sort(key=lambda x: x[1], reverse=True)
    
    print(f"PRIMARY BOTTLENECK:    {subsystems[0][0]} ({subsystems[0][1]:.2f} ms promedio)")
    print(f"SECONDARY BOTTLENECK:  {subsystems[1][0]} ({subsystems[1][1]:.2f} ms promedio)")
    print()
    
    # Scaling analysis
    print("─" * 79)
    print("  ANÁLISIS DE ESCALAMIENTO")
    print("─" * 79)
    print()
    
    # Comparar 500 bullets vs 2000 bullets
    frames_500 = [f for f in frames if 400 <= f['activeProjectiles'] <= 600]
    frames_2000 = [f for f in frames if 1900 <= f['activeProjectiles'] <= 2100]
    
    if frames_500 and frames_2000:
        avg_frame_500 = sum(f['frameTimeMs'] for f in frames_500) / len(frames_500)
        avg_frame_2000 = sum(f['frameTimeMs'] for f in frames_2000) / len(frames_2000)
        
        ratio_bullets = 2000 / 500  # 4x
        ratio_time = avg_frame_2000 / avg_frame_500
        
        print(f"Proyectiles: 500 → 2000 (4x)")
        print(f"Frame time:  {avg_frame_500:.2f} ms → {avg_frame_2000:.2f} ms ({ratio_time:.2f}x)")
        print()
        
        if ratio_time < ratio_bullets * 1.3:
            print("SCALING:  O(n) — escalamiento lineal")
        elif ratio_time < ratio_bullets * 2.5:
            print("SCALING:  O(n log n) — escalamiento subquadrático")
        else:
            print("SCALING:  O(n²) o peor — escalamiento superlineal")
    else:
        print("SCALING:  Datos insuficientes para calcular")
    
    print()
    print("═" * 79)

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("USO: python analyze_profiling_results.py profiling_results.csv")
        sys.exit(1)
    
    csv_path = sys.argv[1]
    analyze_profiling_csv(csv_path)
