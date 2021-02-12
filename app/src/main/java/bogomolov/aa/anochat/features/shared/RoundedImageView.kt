package bogomolov.aa.anochat.features.shared

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Color
import android.util.AttributeSet
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.appcompat.content.res.AppCompatResources
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.databinding.BindingAdapter
import bogomolov.aa.anochat.R
import bogomolov.aa.anochat.repository.getFilePath
import bogomolov.aa.anochat.repository.getMiniPhotoFileName
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
            imageView.setImageDrawable(drawable)
        }
    }

    fun setFile(fileName: String): Boolean {
        val imageView: ImageView = findViewById(R.id.round_image)
        val filePath = getFilePath(context, getMiniPhotoFileName(fileName))
        if (File(filePath).exists()) {
            imageView.setImageBitmap(BitmapFactory.decodeFile(filePath))
            return true
        }
        return false
    }

}

@BindingAdapter("app:srcFile")
fun setFileName(view: RoundedImageView, fileName: String?) {
    if (fileName != null && view.setFile(fileName)) {
        view.requestLayout()
    } else {
        view.setDefaultDrawable()
    }
}