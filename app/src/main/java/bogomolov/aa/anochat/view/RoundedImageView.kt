package bogomolov.aa.anochat.view

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Color
import android.util.AttributeSet
import android.util.Log
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.appcompat.content.res.AppCompatResources
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.databinding.BindingAdapter
import bogomolov.aa.anochat.R
import bogomolov.aa.anochat.android.getFilePath
import bogomolov.aa.anochat.android.getMiniPhotoFileName
import java.io.File


class RoundedImageView(context: Context, attrs: AttributeSet) :
    FrameLayout(context, attrs) {
    var defaultDrawable: Int? = null

    init {
        ConstraintLayout.inflate(context, R.layout.rounded_image_layout, this)

        val a = context.obtainStyledAttributes(attrs, R.styleable.RoundedImageView, 0, 0)

        val tintColor = a.getColor(R.styleable.RoundedImageView_foregroundColor, Color.BLACK)
        val roundFg: ImageView = findViewById(R.id.round_fg_image)
        roundFg.setColorFilter(tintColor)

        defaultDrawable = a.getResourceId(R.styleable.RoundedImageView_srcDrawable, -1);
        setDefaultDrawable()
        val fileName = a.getString(R.styleable.RoundedImageView_srcFile)
        if (fileName != null) setFile(fileName)

        a.recycle()
    }

    fun setDefaultDrawable() {
        if (defaultDrawable != -1) {
            val imageView: ImageView = findViewById(R.id.round_image)
            val drawable = AppCompatResources.getDrawable(context, defaultDrawable!!)
            Log.i("test","setDefaultDrawable")
            imageView.setImageDrawable(drawable)
        }
    }

    fun setFile(fileName: String) {
        val imageView: ImageView = findViewById(R.id.round_image)
        val filePath = getFilePath(context, getMiniPhotoFileName(context,fileName))
        if(File(filePath).exists()){
            Log.i("test","setFile $fileName")
            imageView.setImageBitmap(BitmapFactory.decodeFile(filePath))
        }
    }

}

@BindingAdapter("app:srcFile")
fun setFileName(view: RoundedImageView, fileName: String?) {
    Log.i("test","BindingAdapter app:srcFile $fileName")
    if (!fileName.isNullOrEmpty()) {
        view.setFile(fileName)
        view.requestLayout()
    } else {
        view.setDefaultDrawable()
    }
}