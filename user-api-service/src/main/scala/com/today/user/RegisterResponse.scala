package com.today.user

import java.util.Date

case class RegisterResponse( /**
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
                              * 注册时间
                              *
                              **/

                            created_at: Date)
