# DEMRasterSources benchmark results

To reproduce the table below you can use the following command:

```bash
$ sbt
> project benchmark
> jmh:run -i 10 -wi 5 -f1 -t1 .*DEMRasterSourceBench.*
```

### Read + Resample 3/23/2020

```
# VM options: -Xmx4G
# Cores: 4
```

|                             | Dataset  | Resolution | delaunay + rasterization, ms/op |
|-----------------------------|----------|------------|---------------------------------|
|**PDAL (native rasterizer)** |red-rocks | 6.9375     | **382.476  ±    44.347**        |
|PDAL (JVM rasterizer)        |red-rocks | 6.9375     | 735.026    ±    10.594          |
|GT                           |red-rocks | 6.9375     | 2319.073   ±   171.407          |
|**PDAL (native rasterizer)** |red-rocks | 3.4687     | **5573.016 ±   493.018**        |
|PDAL (JVM rasterizer)        |red-rocks | 3.4687     | 12579.721  ±  2345.998          |
|GT                           |red-rocks | 3.4687     | 88464.543  ± 10692.569          |
|**PDAL (native rasterizer)** |red-rocks | 1.7344     | **8083.490 ±  1786.544**        |
|PDAL (JVM rasterizer)        |red-rocks | 1.7344     | 16152.638  ±  4003.070          |
|GT                           |red-rocks | 1.7344     | 158994.121 ± 23280.155          |
|**PDAL (native rasterizer)** |red-rocks | 0.8672     | **7697.157 ±   396.745**        |
|PDAL (JVM rasterizer)        |red-rocks | 0.8672     | 16710.832  ±  2661.915          |
|GT                           |red-rocks | 0.8672     | 153764.608 ± 12090.790          |

#### Legend:
* PDAL (native rasterizer) - benchmarks a DEMRasterSource that uses PDAL Delaunay and uses native (C++) Rasterizer.
* PDAL (JVM rasterizer) - benchmarks a DEMRasterSource that uses PDAL Delaunay and uses JVM Rasterizer.
* GT - benchmarks a DEMRasterSource that uses GeoTrellis Delaunay and uses JVM Rasterizer.

#### Conclusion

PDAL Delaunay + Native rasterization makes a lot of sense and it is at it behaves at 
least two times faster comparing to other methods. It makes no sense to use GeoTrellis Delaunay Triangulation at this point.

### Reproject 3/23/2020

|                    | Dataset  | Resolution | delaunay + rasterization, ms/op |
|--------------------|----------|------------|---------------------------------|
|PDAL reproject      |red-rocks | 6.9375     | 531.069 ±    47.114             |
|GT region reproject |red-rocks | 6.9375     | **527.233 ± 15.586**            |

#### Legend:
* PDAL reproject - benchmarks reprojection done on the PDAL side via [filters.reprojection](https://pdal.io/stages/filters.reprojection.html).
* GT region reproject - benchmarks reprojection done using GeoTrellis capabilities.

#### Conclusion

GeoTrellis reprojection tends to be a slightly faster. 
The performance is pretty much the same with taking into account the standard deviation. 
We decided to rely on the GeoTrellis reprojection capabilities at this point.

# PDAL JNI Bindings benchmarks

This benchmark is done against [PDAL Java](https://github.com/PDAL/java) bindings to get the sense of what is slow in the DEMRasterSource.
It does not use the `RasterSource` abstraction and uses only [PDAL Java](https://github.com/PDAL/java) API.

To reproduce the table below you can use the following command:

```bash
$ sbt
> project benchmark
> jmh:run -i 10 -wi 5 -f1 -t1 .*ReadEPTBench.*
```

### 3/23/2020

```
# VM options: -Xmx4G
# Cores: 4
```

|                            | Dataset  | Resolution | delaunay + rasterization, ms/op |
|----------------------------|----------|------------|---------------------------------|
|Pipeline execute            |red-rocks | 6.9375     | 207.858 ± 18.166                |
|Pipeline Delaunay execute   |red-rocks | 6.9375     | 388.973 ± 76.595                |
|Pipeline Delaunay rasterize |red-rocks | 6.9375     | 419.780 ± 63.084                |

#### Legend:
* Pipeline execute - benchmarks raw Pipeline execution without generating a triangulation mesh.
* Pipeline Delaunay execute - benchmarks a Pipeline execution with generating a triangulation mesh.
* Pipeline Delaunay rasterize - benchmarks Pipeline execution with generating a triangulation mesh with the mesh rasterization. 
