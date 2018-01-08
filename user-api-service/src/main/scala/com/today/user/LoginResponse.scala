package com.today.user

import java.util.Date

case class LoginResponse( /**
                            *
                            * *
                            * 用户名称
                            *
                            **/

                          user_name: String,

                          /**
                            *
                            * *
                            * 电话号码
                            *
                            **/

                          telephone: String,

                          /**
                            *
                            * *
                            * 用户状态
                            *
                            **/

                          user_status:Int,

                          /**
                            *
                            * *
                            * 用户 积分
                            *
                            **/

                          integral: Int,

                          /**
                            *
                            * *
                            * 注册时间
                            *
                            **/

                          created_at: Date,

                          /**
                            *
                            * *
                            * 更新时间
                            *
                            **/

                          updated_at: Date,

                          /**
                            *
                            * *
                            * 用户邮箱
                            *
                            **/

                          email: Option[String] = None,

                          /**
                            *
                            * *
                            * 用户 qq
                            *
                            **/

                          qq: Option[String] = None)


