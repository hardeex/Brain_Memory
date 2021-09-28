package com.hardextech.brainmemory.models

import com.google.firebase.firestore.PropertyName

data class UserImageListDataClass (
    /*
    A dataclass refers to class that contains only fields and crud method for accessing them (getter and setter). These class not contain additional functionality
    and can not independently operate on the data that they own

    the use of data class is for holding data
     */

// the field name that this dataclass will hold
        @PropertyName("images") val images: List<String>?= null
        )
