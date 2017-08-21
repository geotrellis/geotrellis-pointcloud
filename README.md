# GeoTrellis PointCloud

[![Build Status](https://travis-ci.org/geotrellis/geotrellis-pointcloud.svg?branch=master)](https://travis-ci.org/geotrellis/geotrellis-pointcloud)

GeoTrellis PointCloud uses PDAL bindings to work with PointCloud data.

> PDAL is Point Data Abstraction Library.
> GDAL for point cloud data.
- [pdal.io](https://pdal.io/)

PDAL supports reading pointcloud data in various of formats.
GeoTrellis PDAL allows read PointCloud data in any PDAL supported format into RDDs
and to rasterize this data using. It's also possible to store data as a GeoTrellis layer
without rasterizing, this feature allows to rasterize data on demand.

NOTE: Using the `geotrellis.pdal` module requires a working installation of
PDAL's java bindings.
