package com.fake.soundremote.data.room

import androidx.room.Dao
import androidx.room.Query
import com.fake.soundremote.data.Keystroke
import com.fake.soundremote.data.KeystrokeInfo
import com.fake.soundremote.data.KeystrokeOrder
import kotlinx.coroutines.flow.Flow

@Dao
interface KeystrokeDao : BaseDao<Keystroke> {
    @Query("SELECT * FROM ${Keystroke.TABLE_NAME} WHERE ${Keystroke.COLUMN_ID} = :id")
    suspend fun getById(id: Int): Keystroke?

    @Query("DELETE FROM ${Keystroke.TABLE_NAME} WHERE ${Keystroke.COLUMN_ID} = :id")
    suspend fun deleteById(id: Int)

    @Query(
        """
            UPDATE ${Keystroke.TABLE_NAME}
            SET ${Keystroke.COLUMN_FAVOURED} = :favoured
            WHERE ${Keystroke.COLUMN_ID} = :id;
        """
    )
    suspend fun changeFavoured(id: Int, favoured: Boolean)

    @Query(
        """
        SELECT k.* FROM ${Keystroke.TABLE_NAME} AS k
        JOIN ${KeystrokeOrder.TABLE_NAME} AS o
        ON o.${KeystrokeOrder.COLUMN_KEYSTROKE_ID} = k.${Keystroke.COLUMN_ID}
        ORDER BY o.'${KeystrokeOrder.COLUMN_ORDER}' DESC;
        """
    )
    suspend fun getOrderedOneshot(): List<Keystroke>

    @Query(
        """
        SELECT 
        ${Keystroke.COLUMN_ID},
        ${Keystroke.COLUMN_KEY_CODE},
        ${Keystroke.COLUMN_MODS},
        ${Keystroke.COLUMN_NAME}
        FROM ${Keystroke.TABLE_NAME} AS k
        JOIN ${KeystrokeOrder.TABLE_NAME} AS o
        ON o.${KeystrokeOrder.COLUMN_KEYSTROKE_ID} = k.${Keystroke.COLUMN_ID}
        WHERE ${Keystroke.COLUMN_FAVOURED} = :favoured
        ORDER BY o.'${KeystrokeOrder.COLUMN_ORDER}' DESC; 
        """
    )
    fun getFavouredOrdered(favoured: Boolean): Flow<List<KeystrokeInfo>>

    @Query(
        """
        SELECT k.* FROM ${Keystroke.TABLE_NAME} AS k
        JOIN ${KeystrokeOrder.TABLE_NAME} AS o
        ON o.${KeystrokeOrder.COLUMN_KEYSTROKE_ID} = k.${Keystroke.COLUMN_ID}
        ORDER BY o.'${KeystrokeOrder.COLUMN_ORDER}' DESC;
        """
    )
    fun getAllOrdered(): Flow<List<Keystroke>>
}