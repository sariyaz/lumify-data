class ffmpeg::libogg($prefix="/usr/local", $tmpdir="/usr/local/src") {
  require buildtools
  include macro

  $srcdir = "${tmpdir}/libogg-1.3.1"

  macro::download { "libogg-download":
    url     => "http://downloads.xiph.org/releases/ogg/libogg-1.3.1.tar.gz",
    path    => "${tmpdir}/libogg-1.3.1.tar.gz",
  } -> macro::extract { 'extract-libogg':
    file    => "${tmpdir}/libogg-1.3.1.tar.gz",
    path    => $tmpdir,
    creates => $srcdir,
  }

  $configure  = "${srcdir}/configure --prefix=${prefix}"
  $make       = "/usr/bin/make -j${processorcount}"
  $install    = "/usr/bin/make install"
  $distclean  = "/usr/bin/make distclean"
  $cmd        = "${configure} && ${make} && ${install} && ${distclean}"

  exec { 'libogg-build' :
    cwd => $srcdir,
    command => $cmd,
    creates => "${prefix}/lib/libogg.a",
    require => Macro::Extract['extract-libogg'],
  }
}
