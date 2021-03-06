class ffmpeg::libtheora($prefix="/usr/local", $tmpdir="/usr/local/src") {
  require buildtools
  require libogg
  include macro

  $srcdir = "${tmpdir}/libtheora-1.1.1"

  macro::download { "libtheora-download":
    url     => "http://downloads.xiph.org/releases/theora/libtheora-1.1.1.tar.gz",
    path    => "${tmpdir}/libtheora-1.1.1.tar.gz",
  } -> macro::extract { 'extract-libtheora':
    file    => "${tmpdir}/libtheora-1.1.1.tar.gz",
    path    => $tmpdir,
    creates => $srcdir,
  }

  $configure  = "${srcdir}/configure --prefix=${prefix} --with-ogg=${prefix} --disable-examples --disable-sdltest --disable-vorbistest"
  $make       = "/usr/bin/make -j${processorcount}"
  $install    = "/usr/bin/make install"
  $distclean  = "/usr/bin/make distclean"
  $cmd        = "${configure} && ${make} && ${install} && ${distclean}"

  exec { 'libtheora-build' :
    cwd => $srcdir,
    command => $cmd,
    environment => "LD_LIBRARY_PATH=\$LD_LIBRARY_PATH:/${prefix}/lib",
    creates => "${prefix}/lib/libtheora.a",
    require => Macro::Extract['extract-libtheora'],
  }
}
