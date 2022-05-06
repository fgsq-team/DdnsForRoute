# route ddns

#### 项目介绍
java实现动态域名解析，使用到了阿里云解析api，获取外网ip是爬取路由器的外网ip(光猫要设置路由拨号)
现在已实现爬取小米路由和openwrt
此项目调用阿里云api参照 [AliYunDdns](https://gitee.com/ienai/AliYunDdns/tree/master)。
#### 软件架构
maven项目 
从路由器上爬取本机的公网ip
使用阿里云api 解析域名
#### 使用教程
修改配置文件
