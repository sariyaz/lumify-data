class zookeeper {
  package { 'hadoop-zookeeper-server':
    ensure  => installed,
    require => Package['hadoop-0.20'],
  }
}