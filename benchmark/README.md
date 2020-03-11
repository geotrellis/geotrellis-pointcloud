# DEMRasterSources benchmark results

To reproduce the table below you can use the following command:

```bash
$ sbt
> project benchmark
> jmh:run -i 10 -wi 5 -f1 -t1 .*DEMRasterSourceBench.*
```

### 3/11/2020

```
# VM options: -Xmx4G
# Cores: 4
```

|                             | Dataset  | Resolution | delaunay + rasterization, ms/op |
|-----------------------------|----------|------------|---------------------------------|
|**PDAL (native rasterizer)** |red-rocks | 6.9375     | **878.513    ±   102.391**      |
|PDAL (JVM rasterizer)        |red-rocks | 6.9375     | 1877.393   ±  1127.682          |
|GT                           |red-rocks | 6.9375     | 3301.495   ±   846.053          |
|**PDAL (native rasterizer)** |red-rocks | 3.4687     | **6223.862   ±   610.427**      |
|PDAL (JVM rasterizer)        |red-rocks | 3.4687     | 13023.915  ±  1695.589          |
|GT                           |red-rocks | 3.4687     | 95570.479  ± 11310.479          |
|**PDAL (native rasterizer)** |red-rocks | 1.7344     | **11113.148  ±  5216.283**      |
|PDAL (JVM rasterizer)        |red-rocks | 1.7344     | 15685.002  ±  1306.871          |
|GT                           |red-rocks | 1.7344     | 179422.928 ± 22267.424          |
|**PDAL (native rasterizer)** |red-rocks | 0.8672     | **10907.534  ±  2789.634**      |
|PDAL (JVM rasterizer)        |red-rocks | 0.8672     | 20032.444  ±  9338.665          |
|GT                           |red-rocks | 0.8672     | 204982.543 ± 86162.431          |

#### Legend:
* PDAL (native rasterizer) - benchmarks a DEMRasterSource that uses PDAL Delaunay and uses native (C++) Rasterizer.
* PDAL (JVM rasterizer) - benchmarks a DEMRasterSource that uses PDAL Delaunay and uses JVM Rasterizer.
* GT - benchmarks a DEMRasterSource that uses GeoTrellis Delaunay and uses JVM Rasterizer.
