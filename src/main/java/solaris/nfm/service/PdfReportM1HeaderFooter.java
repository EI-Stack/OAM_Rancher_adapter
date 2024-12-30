package solaris.nfm.service;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;

import java.io.IOException;

public class PdfReportM1HeaderFooter extends PdfPageEventHelper
{
	/**
	 * 頁眉
	 */
	public String		header			= "";
	/**
	 * 文檔字體大小，頁腳頁眉最好和文本大小一致
	 */
	public int			presentFontSize	= 12;
	/**
	 * 文檔頁面大小，最好前面傳入，否則默認爲A4紙張
	 */
	public Rectangle	pageSize		= PageSize.A4;
	// 模板
	public PdfTemplate	total			= null;
	// 基礎字體對象
	public BaseFont		bf				= null;
	// 利用基礎字體生成的字體對象，一般用於生成中文文字
	public Font			fontDetail		= null;

	public PdfReportM1HeaderFooter()
	{

	}

	/**
	 * @param yeMei
	 *        頁眉字符串
	 * @param presentFontSize
	 *        數據體字體大小
	 * @param pageSize
	 *        頁面文檔大小，A4，A5，A6橫轉翻轉等Rectangle對象
	 */
	public PdfReportM1HeaderFooter(final String yeMei, final int presentFontSize, final Rectangle pageSize)
	{
		this.header = yeMei;
		this.presentFontSize = presentFontSize;
		this.pageSize = pageSize;
	}

	public void setHeader(final String header)
	{
		this.header = header;
	}

	public void setPresentFontSize(final int presentFontSize)
	{
		this.presentFontSize = presentFontSize;
	}

	/**
	 * TODO 文檔打開時創建模板
	 */
	@Override
	public void onOpenDocument(final PdfWriter writer, final Document document)
	{
		total = writer.getDirectContent().createTemplate(50, 50);// 共頁的矩形的長寬高
	}

	/**
	 * TODO 關閉每頁的時候，寫入頁眉，寫入'第幾頁共'這幾個字。
	 */
	@Override
	public void onEndPage(final PdfWriter writer, final Document document)
	{
		try
		{
			if (bf == null)
			{
				bf = BaseFont.createFont("STSong-Light", "UniGB-UCS2-H", BaseFont.NOT_EMBEDDED);
			}
			if (fontDetail == null)
			{
				fontDetail = new Font(bf, presentFontSize, Font.NORMAL);// 數據體字體
			}
		} catch (final DocumentException e)
		{
			e.printStackTrace();
		} catch (final IOException e)
		{
			e.printStackTrace();
		}

		// 拿到當前的PdfContentByte
		final PdfContentByte cb = writer.getDirectContent();

		// 1.寫入頁眉 參數：要寫入文本的頁面對象，對齊，文字內容，X軸位置，Y軸位置，逆時針旋轉的角度
		ColumnText.showTextAligned(cb, Element.ALIGN_LEFT, new Phrase(header, fontDetail), document.left(), document.top() + 20, 45);

		// 2.寫入頁腳 前半部分的 第 X頁/共
		final int pageS = writer.getPageNumber();
		final String foot1 = "第 " + pageS + " 頁 /共";
		final Phrase footer = new Phrase(foot1, fontDetail);

		// 計算前半部分的foot1的長度，後面好定位最後一部分的'Y頁'這倆字的x軸座標，字體長度也要計算進去 = len
		final float len = bf.getWidthPoint(foot1, presentFontSize);

		/**
		 * 寫入頁腳 前半部分的 第 X頁/共
		 * x軸就是(右margin+左margin + right() -left()- len)/2.0F 再給偏移20F適合人類視覺感受，否則肉眼看上去就太偏左了
		 * y軸就是底邊界-20,否則就貼邊重疊到數據體裏了就不是頁腳了；注意Y軸是從下往上累加的，最上方的Top值是大於Bottom好幾百開外的。
		 */
		ColumnText.showTextAligned(cb, Element.ALIGN_CENTER, footer, (document.rightMargin() + document.right() + document.leftMargin() - document.left() - len) / 2.0F + 20F, document.bottom() - 20,
				0);

		/**
		 * 寫入頁腳 後半部分 ？頁
		 * 注：因爲共？頁 中的？值是在doc.close() 後才能得到，所以這一先加入模板，最後結束的時候後邊用值替換
		 * x=(右margin+左margin + right() -left())/2.0F
		 * y 軸和之前的保持一致，底邊界-20
		 */
		cb.addTemplate(total, (document.rightMargin() + document.right() + document.leftMargin() - document.left()) / 2.0F + 20F, document.bottom() - 20); // 調節模版顯示的位置
	}

	/**
	 * TODO 關閉文檔時，替換模板，完成整個頁眉頁腳組件
	 */
	@Override
	public void onCloseDocument(final PdfWriter writer, final Document document)
	{
		// 7.最後一步了，就是關閉文檔的時候，將模板替換成實際的 Y 值,至此，page x of y 製作完畢，完美兼容各種文檔size。
		total.beginText();
		total.setFontAndSize(bf, presentFontSize);// 生成的模版的字體、顏色
		final String foot2 = " " + writer.getPageNumber() + " 頁";
		total.showText(foot2);// 模版顯示的內容
		total.endText();
		total.closePath();
	}
}