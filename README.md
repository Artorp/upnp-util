# upnp-util
A simple CLI program for UPnP.

Can be used to fetch existing UPnP port mappings from connected gateways. Or create new mappings.

Makes use of the [weupnp](https://github.com/bitletorg/weupnp) java library.

## Usage

Download the latest version from the [releases](https://github.com/Artorp/upnp-util/releases) page. Start the program by running

```
java -jar <jarfile>
```

The default options are listed below

```
usage: upnp-util [OPTIONS]
 -h,--help      print this message
 -r,--run       run the port mapper wizard
 -v,--version   print the version information and exit
Example:
    upnp-util -h
```
