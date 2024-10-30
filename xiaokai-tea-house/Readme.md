# 小楷食坊(Xiaokai TEA HOUSE)

使用JDK21、nginx1.20.2

本项目约定：

- **管理端**发出的请求，统一使用 **/admin** 作为前缀。
- **用户端**发出的请求，统一使用 **/user** 作为前缀。
- 接口文档: http://localhost:8080/doc.html#/ 
- 起始地址: http://localhost/#/login

## 实现功能
### 大功能修改
- 完成[苍穹外卖](https://github.com/shuhongfan/xiaokai-tea-house)所有功能，微信小程序下单，以及订单等增删改查
- 修改OSS服务器储存图片名，每次调用图片时，通过图片名获取图片(OSS服务器自动设置过期时间，在过期时间内再次请求，文件名不会改变)
- Redis基于Cacheable注解过期缓存时间以及缓存刷新时间修改

### 小功能修改
- 将地址管理部分第一条初始化地址设置为默认地址，且默认地址删除后挑选一个作为默认地址
- 百度地图API转为更加广泛使用的高德地图API
- 使用Scheduled自动为商家接单
- 地址管理中修改地址加入修改省市区编码，以防出现名字与编码不一致情况


## 1. 运行前端程序
- 进入前端项目目录nginx-1.20.2
- 运行nginx.exe
- （直接运行，关闭的话可能会有多个无法关闭），使用如下命令全部关闭：
    ``` cmd
    进入nginx文件夹查看nginx进程
    tasklist /fi "IMAGENAME eq nginx.exe"
    杀死所有nginx进程
    taskkill /f /t /im nginx.exe
    ```
    
- 如果需要修改的，进入vue-front使用如下命令安装好包

```cmd
npm run serve
```

- 再使用如下命令运行

```cmd
npm run serve
```



## 2. 新建数据库
- 连接mysql
- 进入init\database文件夹，运行xiaokai.sql语句
- dish图片路径已经修改为*.png，因此需要将init\img\*里面图片全部上传到OSS服务器，名字不用变化
- 需要在.\xiaokai-tea-house\xiaokai-server\src\main\resources\application-dev.yml修改为数据库中登录名和密码
- 管理员密码已经改成加密后数据，加密前为123456

## 3. 导入接口
- 将init\project-interface文件夹下json文件分别导入到[apifox](https://app.apifox.com)中
- 原来使用的是yapi，但是现在停止维护后可以使用apifox替代

## 4.云服务器
- 在阿里云申请oss服务，并且修改.\xiaokai-tea-house\xiaokai-tea-house\xiaokai-server\src\main\resources\application-dev.yml目录下的alioss相关配置

## 5.Redis
- 需要在Linux安装Redis
- 卸载原有Redis(可选)
  1. 停止 Redis 服务（如果 Redis 正在运行）： 
  ```
  sudo systemctl stop redis
  ```
  2. 删除 Redis 二进制文件：
  ```
  sudo rm /usr/local/bin/redis-server
  sudo rm /usr/local/bin/redis-cli
  sudo rm /usr/local/bin/redis-benchmark
  sudo rm /usr/local/bin/redis-check-aof
  sudo rm /usr/local/bin/redis-sentinel
  sudo rm /usr/local/bin/redis-check-rdb
  ```
  3. 删除 Redis 配置和数据目录（如果安装时指定了这些目录）：
  ```
  sudo rm -rf /etc/redis
  sudo rm -rf /var/lib/redis
  ```
  4. 删除 Redis 安装目录（假设你在 /usr/local/redis 安装）：
  ```
  sudo rm -rf /usr/local/redis
  ```
- 在Linux系统安装Redis步骤：
  1. 下载[Redis](https://download.redis.io/releases/)安装包
  2. 将Redis安装包上传到Linux
  3. 解压安装包到data1，命令：tar -zxvf redis-x.tar.gz -C /data1
  4. 安装Redis的依赖环境gcc，命令：yum install gcc-c++
  5. 进入/data1/redis-x，进行编译，命令：make
  6. 进入/data1/redis-x，进行编译，命令：make install
- 安装后重点文件说明：
  - /data1/redis-x/src/redis-server：Redis服务启动脚本
  - /data1/redis-x/src/redis-cli：Redis客户端脚本
  - /data1/redis-x/redis.conf：Redis配置文件
- 指定配置启动
  如果要让Redis以后台方式启动，则必须修改Redis配置文件，就在之前解压的redis安装包下
  （`/data1/redis-x`），名字叫redis.conf。先将这个配置文件备份一份：
  ```
  cp redis.conf redis.conf.bck
  ```
- 修改redis.conf文件中的一些配置：
  ```properties
  # 监听的地址，默认是127.0.0.1，会导致只能在本地访问。修改为0.0.0.0则可以在任意IP访问，生产环境不要设置为0.0.0.0
  bind 0.0.0.0
  # 守护进程，修改为yes后即可后台运行
  daemonize yes 
  # 密码，设置后访问Redis必须输入密码
  requirepass 123321
  ```

- 其它常见配置：

  ```properties
  # 监听的端口
  port 6379
  # 工作目录，默认是当前目录，也就是运行redis-server时的命令，日志、持久化等文件会保存在这个目录
  dir .
  # 数据库数量，设置为1，代表只使用1个库，默认有16个库，编号0~15
  databases 1
  # 设置redis能够使用的最大内存
  maxmemory 512mb
  # 日志文件，默认为空，不记录日志，可以指定日志文件名
  logfile "redis.log"
  ```

- 启动Redis：
  ```sh
  # 进入redis安装目录 
  cd /data1/redis-x
  # 启动
  redis-server redis.conf
  ```

- 查看是否启动:
  ```
  ps -ef | grep redis
  ```

- 停止服务：
  
  ```sh
  # 利用redis-cli来执行 shutdown 命令，即可停止 Redis 服务，
  # 因为之前配置了密码，因此需要通过 -u 来指定密码
  redis-cli -u 123321 shutdown
  ```

- 需要在.\xiaokai-tea-house\xiaokai-server\src\main\resources\application-dev.yml修改自己登录名和密码的redis信息

## 6. 微信小程序
- 小程序项目代码位于.\xiaokai-tea-house\wechat-small-program
- 对于不同的ip和端口号，需要修改.\xiaokai-tea-house\xiaokai-tea-house\wechat-small-program\common\vendor.js中baseUrl
- 需要在.\xiaokai-tea-house\xiaokai-server\src\main\resources\application-dev.yml修改自己微信id和secret信息

## 7. 临时公网ip访问准备
- 下载[cpolar](https://dashboard.cpolar.com/get-started)
- 进入安装文件夹运行cpolar.exe
- 在官网注册账号，进入[验证分区](https://dashboard.cpolar.com/auth) 复制隧道Authtoken
- 在cpolar.exe输入如下命令
  ```sh
  cpolar.exe authtoken <your-authtoken>
  ```
- 最后执行如下命令，将80端口映射到公网
  ```sh
  cpolar.exe http 80
  ```
- 最后在浏览器输入公网ip(Forward)即可访问
  ```sh
  # 比如
  Forwarding          http://5d787fa4.r23.cpolar.top -> http://localhost:80
  ```

## 8. 申请高德开放平台地图Web服务
- [高德开放平台地图API](https://console.amap.com/dev/key/app)创建对应Key，注意不用开启数字签名，否则还要更新代码调用方式

## Tips
- 每次重启后需要执行如下命令
  - 本地 
    - 启动mysql，并且登录mysql 
    ```sh
    net start mysql
    mysql -uroot -p # 输入个人mysql密码：1234
    ```
    - 打开navicat，localhost密码1234
    - 进入项目文件夹打开前端页面启动器：nginx.exe
    - 进入cpolar安装文件夹运行cpolar.exe映射端口
    ```sh
    cpolar.exe http 80
    ```
  - 虚拟机中(个人虚拟机密码：123456)
    - redis
    ```
    cd /data1/redis-x  # 进入redis安装目录
    redis-server redis.conf  # 启动
    ```


## TODO
- 小程序写死的首页信息通过接口获取
- 超出配送范围的地址，前端需要提示
- 数据面板数据统计不计算今天，从昨天开始计算
- 登录使用手机号和验证码并放到redis
