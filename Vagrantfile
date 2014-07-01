# -*- mode: ruby -*-
# vi: set ft=ruby :

HOSTNAME = 'lumify-vm.lumify.io'
DEFAULT_PRIVATE_NETWORK_IP = '192.168.33.10'

def format_script(input)
  input.split(/\n/).collect do |line|
    line.strip!
    line.length > 0 ? line : nil
  end.compact.join("\n")
end

def private_network_ip
  ENV['PRIVATE_NETWORK_IP'] || DEFAULT_PRIVATE_NETWORK_IP
end

def ensure_private_network
  three_octets = private_network_ip.match(/(\d+\.\d+\.\d+)\.\d+/).captures[0]
  unless `VBoxManage list hostonlyifs`.lines.any? {|line| line.match(three_octets)}
    new_if = `VBoxManage hostonlyif create`.match(/Interface '(.*)' was successfully created/).captures[0]
    `VBoxManage hostonlyif ipconfig "#{new_if}" --ip #{three_octets}.1`
  end
end

def configure_network(config)
  config.vm.network :forwarded_port, :guest => 8080, :host => 8080, :auto_correct => true
  config.vm.network :forwarded_port, :guest => 8443, :host => 8443, :auto_correct => true
  config.vm.network :private_network, :ip => private_network_ip
  script = """
    hostname #{HOSTNAME}
    sed -i -e '/^#{private_network_ip}/ d' /etc/hosts
    echo '#{private_network_ip} #{HOSTNAME.split('.').first} #{HOSTNAME}' >> /etc/hosts
  """
  config.vm.provision :shell, :inline => "echo 'configure hostname and /etc/hosts'\n" + format_script(script)
end

def provision_proxy(config, proxy_url)
  if proxy_url
    protocol, host, port = proxy_url.match(/(.+):\/\/(.+):(\d+)/).captures
    settings_xml = """
      <settings>
        <proxies>
          <proxy>
            <active>true</active>
            <protocol>#{protocol}</protocol>
            <host>#{host}</host>
            <port>#{port}</port>
          </proxy>
        </proxies>
      </settings>
    """
    script = """
      echo 'proxy=#{proxy_url}' >> /etc/yum.conf
      for repo in /etc/yum.repos.d/*.repo; do grep -q 'mirrorlist=' ${repo} && sed -i -e 's/mirrorlist=/#mirrorlist=/' -e 's/#*baseurl=/baseurl=/' ${repo}; done
      echo 'registry = http://registry.npmjs.org/' >> /usr/etc/npmrc
      echo 'proxy = #{proxy_url}' >> /usr/etc/npmrc
    """
    config.vm.provision :shell, :inline => "echo 'enable yum and npm proxies'\n" + format_script(script)
    config.vm.provision :shell, :inline => "echo 'enable mvn proxy'; mkdir -p ${HOME}/.m2 && echo '#{settings_xml}' > ${HOME}/.m2/settings.xml", :privileged => false
  else
    script = """
      sed -i -e '/^proxy=/d' /etc/yum.conf
      for repo in /etc/yum.repos.d/*.repo; do grep -q 'mirrorlist=' && sed -i -e 's/#*mirrorlist=/mirrorlist=/' -e 's/baseurl=/#baseurl=/' ${repo}; done
      [ -f /usr/etc/npmrc ] && sed -i -e '/^registry =/d' /usr/etc/npmrc || true
      [ -f /usr/etc/npmrc ] && sed -i -e '/^proxy =/d' /usr/etc/npmrc || true
    """
    config.vm.provision :shell, :inline => "echo 'disable yum and npm proxies'\n" + format_script(script)
    config.vm.provision :shell, :inline => "echo 'disable mvn proxy'; rm -f ${HOME}/.m2/settings.xml", :privileged => false
  end
end

def install_puppet_modules(config, module_names)
  script = module_names.collect do |module_name|
    "puppet module list | grep -q #{module_name} || puppet module install #{module_name}"
  end
  config.vm.provision :shell, :inline => "echo 'install puppet module(s)'\n" + script.join("\n")
end

def configure_puppet(puppet, manifest_file, proxy_url=nil)
  puppet.manifests_path      = 'puppet/manifests'
  puppet.module_path         = [ 'puppet/modules', 'puppet/puppet-modules' ]
  puppet.hiera_config_path   = 'puppet/hiera-vm.yaml'
  puppet.manifest_file       = manifest_file
  puppet.facter['fqdn']      = HOSTNAME
  puppet.facter['proxy_url'] = proxy_url if proxy_url
end

Vagrant.configure('2') do |config|
  if RbConfig::CONFIG['host_cpu'] == 'i386' || (ENV['VM_ARCH'] && ENV['VM_ARCH'] == 'i386')
    config.vm.box = 'centos6.4-i386'
    config.vm.box_url = 'http://developer.nrel.gov/downloads/vagrant-boxes/CentOS-6.4-i386-v20130731.box'
  else
    config.vm.box = 'centos6.4'
    config.vm.box_url = 'http://developer.nrel.gov/downloads/vagrant-boxes/CentOS-6.4-x86_64-v20130427.box'
  end

  # config.vm.hostname = HOSTNAME

  # Create a forwarded port mapping which allows access to a specific port
  # within the machine from a port on the host machine. In the example below,
  # accessing "localhost:8080" will access port 80 on the guest machine.
  # config.vm.network :forwarded_port, :guest => 8080, :host => 8080
  # config.vm.network :forwarded_port, :guest => 8443, :host => 8443

  # Create a private network, which allows host-only access to the machine
  # using a specific IP.
  # config.vm.network :private_network, :ip => '192.168.33.10'

  # Create a public network, which generally matched to bridged network.
  # Bridged networks make the machine appear as another physical device on
  # your network.
  # config.vm.network :public_network

  # Share an additional folder to the guest VM. The first argument is
  # the path on the host to the actual folder. The second argument is
  # the path on the guest to mount the folder. And the optional third
  # argument is a set of non-required options.
  # config.vm.synced_folder "../data", "/vagrant_data"

  # Provider-specific configuration so you can fine-tune various
  # backing providers for Vagrant. These expose provider-specific options.
  # Example for VirtualBox:
  #
  config.vm.provider :virtualbox do |vb|
    ensure_private_network
    vb.customize ['modifyvm', :id, '--memory', '4096']
    vb.customize ['modifyvm', :id, '--cpus', '2']
  end
  #
  # View the documentation for the provider you're using for more
  # information on available options.

  # used to compile our dependencies
  config.vm.define 'rpm' do |rpm|
    rpm.vm.provision :shell, :path => 'lumify-rpms/configure-vm.sh'
  end

  # used to manage the local SMMC cluster
  config.vm.define 'puppet' do |puppet|
    puppet.vm.hostname = 'puppet'
    puppet.vm.network :public_network, :ip => '10.0.1.200'
    script = """
      yum install -y git
      cd /vagrant/deployment && ./push.sh - physical/smmc_hosts
      cd && ./init.sh smmc_hosts local
    """
    puppet.vm.provision :shell, :inline => format_script(script)
  end

  # used for development including closed source enterprise features
  config.vm.define 'dev', :primary => true do |dev|
    configure_network(dev)
    provision_proxy(dev, ENV['PROXY_URL'])
    dev.vm.provision :shell, :inline => 'set -x; mkdir -p /data0'
    install_puppet_modules(dev, ['puppetlabs-stdlib'])
    dev.vm.provision :puppet do |puppet|
      configure_puppet(puppet, 'dev_vm.pp', ENV['PROXY_URL'])
    end
  end

  # used for QL integration development
  config.vm.define 'ql' do |ql|
    configure_network(ql)
    provision_proxy(ql, ENV['PROXY_URL'])
    install_puppet_modules(ql, ['puppetlabs-mysql'])
    ql.vm.provision :puppet do |puppet|
      configure_puppet(puppet, 'ql_vm.pp', ENV['PROXY_URL'])
    end
    ql.vm.provision :shell, :path => 'demo-vm/set-property.sh', :args => 'objectdetection.opencv.disabled=true'
    ql.vm.provision :shell, :path => 'demo-vm/set-property.sh', :args => 'clavin.disabled=true'
  end

  # used for automated integration testing
  config.vm.define 'test' do |test|
    configure_network(test)
    provision_proxy(test, ENV['PROXY_URL'])
    test.vm.provision :shell, :inline => 'set -x; mkdir -p /data0'
    test.vm.provision :puppet do |puppet|
      configure_puppet(puppet, 'dev_vm.pp', ENV['PROXY_URL'])
    end
    test.vm.provision :shell, :path => 'bin/test/clone.sh', :args => '/tmp/lumify-all', :privileged => false
    test.vm.provision :shell, :path => 'bin/test/ingest.sh', :args => '/tmp/lumify-all', :privileged => false
  end

  # used to create the downloadable open source demo VM
  config.vm.define 'demo-opensource' do |demo|
    configure_network(demo)
    provision_proxy(demo, ENV['PROXY_URL'])
    demo.vm.provision :shell, :inline => 'set -x; mkdir -p /data0'
    demo.vm.provision :puppet do |puppet|
      configure_puppet(puppet, 'demo_opensource_vm.pp', ENV['PROXY_URL'])
    end
    demo.vm.provision :shell, :path => 'demo-vm/set-property.sh', :args => 'objectdetection.opencv.disabled=true'
    demo.vm.provision :shell, :path => 'demo-vm/set-property.sh', :args => 'clavin.disabled=true'
    demo.vm.provision :shell, :path => 'demo-vm/ingest.sh', :args => 'demo-vm/data/sample-data-html.tgz', :privileged => false
    demo.vm.provision :shell, :path => 'demo-vm/configure-vm.sh'
    demo.vm.provision :shell, :path => 'demo-vm/clean-vm.sh'
  end

  # used to create an enterprise demo VM
  config.vm.define 'demo-enterprise' do |demo|
    configure_network(demo)
    provision_proxy(demo, ENV['PROXY_URL'])
    demo.vm.provision :shell, :inline => 'set -x; mkdir -p /data0'
    demo.vm.provision :puppet do |puppet|
      configure_puppet(puppet, 'demo_enterprise_vm.pp', ENV['PROXY_URL'])
    end
    demo.vm.provision :shell, :path => 'demo-vm/set-property.sh', :args => 'clavin.disabled=true'
    demo.vm.provision :shell, :path => 'demo-vm/ingest.sh', :args => 'demo-vm/data/chechen-terrorists.tgz', :privileged => false
    demo.vm.provision :shell, :path => 'demo-vm/configure-vm.sh'
    demo.vm.provision :shell, :path => 'demo-vm/clean-vm.sh'
  end
end
