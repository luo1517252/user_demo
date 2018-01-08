package com.today.user

import java.util.Date

case class Users (

                   id:Int,

                   user_name:String,

                   password:String,

                   telephone:String,

                   user_status:Int ,

                   email:Option[String] = None,

                   qq:Option[String] = None ,

                   integral:Int,

                   created_at:Date,

                   created_by:Int,

                   updated_at:Date,

                   updated_by:Int,

                   remark:String,

                   is_delete:Int

                 )
