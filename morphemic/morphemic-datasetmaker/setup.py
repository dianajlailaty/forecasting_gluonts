from setuptools import setup

setup(
   name = 'datasetmaker',
   version = '0.0.1',
   author = 'Jean-Didier Totow',
   author_email = 'totow@unipi.gr',
   packages = ['morphemic', 'morphemic.dataset'],
   scripts = [],
   url='http://git.dac.ds.unipi.gr/morphemic/datasetmaker',
   license='LICENSE.txt',
   description='Python package for creating a dataset using InfluxDB data points',
   long_description=open('README.txt').read(),
   install_requires=[
       "pandas",
       "influxdb",
   ],
)
