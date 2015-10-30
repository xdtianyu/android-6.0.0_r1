WKTHMLTOPDF README
==================
Or, how I stopped hating the cdd and learned to love html-to-pdf conversions.


OVERVIEW
==================
TL:DR This document describes how to create a CDD.pdf from the CDD.html file. You need to generate a cover file and a body file, then use Adobe Acrobat to insert the cover page into the body.pdf.

The Android Compatibilty Definition Document (CDD) is maintained as an html file but distributed as a .pdf. The partner team updates the CDD for every new major Android release and the APE doc team posts the new .pdf to source.android.com in http://source.android.com/compatibility/.

To create the pdf from the html file, use wkhtmltopdf (http://wkhtmltopdf.org/) plus a little bit of Adobe Acrobat. You can do the conversion on a Mac or Linux (or even Windows); you just need to download the wkhtmltopdf pkg for your system. However, since you must use Adobe Acrobat Pro (not Reader) to insert the cover page, you must perform this step on a Mac or Windows box (none of the Linux PDF apps can do the swap successfully and still maintain the PDF bookmarks and links)


1. INSTALL WKHTMLTOPDF
====================
Go to http://wkhtmltopdf.org/ and download the app for your system. It's command line only.


2. GENERATE COVER PDF
======================

Syntax:

wkthmltopdf [page-size] [page-margins] cover path-to-html path-to-pdf

page-size
Set to letter.
Ex. -s letter

page-margins
set to 0in (cover goes all the way to page edges)
Ex. -B 0in -T 0in -L 0in -R 0in

path-to-html
The full path (from root, including the filename) to the cover html file. You will need to update the cover text to specify the release name , number, and date. You might also need to swap the image out for the image associated with the release (store images in compatibility/images).
Ex:
/usr/local/google/home/hvm/Projects/internal/lmp-mr1-dev/docs/source.android.com/src/compatibility/5.1/android-cdd-cover_5_1.html

path-to-pdf
The full path (from root, including the filename) to where you want the output pdf file to reside. If the pdf file is NOT open (in Preview or some other app), running the command will silently overwrite the existing .pdf.
Ex.
/usr/local/google/home/hvm/Projects/internal/lmp-mr1-dev/docs/source.android.com/src/compatibility/5.1/android-cdd-cover.pdf


Example cover command:

wkhtmltopdf -s letter -B 0in -T 0in -L 0in -R 0in cover /usr/local/google/home/hvm/Projects/internal/lmp-mr1-dev/docs/source.android.com/src/compatibility/5.1/android-cdd-cover_5_1.html /usr/local/google/home/hvm/Projects/internal/lmp-mr1-dev/docs/source.android.com/src/compatibility/5.1/android-cdd-cover.pdf


3. GENERATE BODY PDF
====================
Syntax:

wkthmltopdf [page-margins] page path-to-html path-to-footer path-to-pdf

page-margins
set to 1in on top and bottom, .75in on left and right.
Ex. -B 1in -T 1in -L .75in -R .75in

path-to-html
The full path (from root, including the filename) to the body html file. This is the main cdd.html, which will change with each release.
Ex.
/usr/local/google/home/hvm/Projects/internal/lmp-mr1-dev/docs/source.android.com/src/compatibility/5.1/android-5.1-cdd.html

path-to-footer
The full path (from root, including the filename) to the footer html file. This is a simple html file that contains the android logo and some javascript to calculate the page number and count. The footer should NOT change from release to release.
Ex.
--footer-html /usr/local/google/home/hvm/Projects/internal/lmp-mr1-dev/docs/source.android.com/src/compatibility/5.1/android-cdd-footer.html


path-to-pdf
The full path (from root, including the filename) to where you want the output pdf file to reside. If the pdf file is NOT open (in Preview or some other app), running the command will silently overwrite the existing .pdf.
Ex.
/usr/local/google/home/hvm/Projects/internal/lmp-mr1-dev/docs/source.android.com/src/compatibility/5.1/android-cdd-body.pdf


Example body command:

wkhtmltopdf -B 1in -T 1in -L .75in -R .75in page /usr/local/google/home/hvm/Projects/internal/lmp-mr1-dev/docs/source.android.com/src/compatibility/5.1/android-5.1-cdd.html --footer-html /usr/local/google/home/hvm/Projects/internal/lmp-mr1-dev/docs/source.android.com/src/compatibility/5.1/android-cdd-footer.html /usr/local/google/home/hvm/Projects/internal/lmp-mr1-dev/docs/source.android.com/src/compatibility/5.1/android-cdd-body.pdf

4. CREATE CSS PDF
==================
A. Open the body.pdf in Adobe Acrobat Pro (you *cannot* use Acrobat Reader for this task).
B. Select Tools > Replace, then open the cover.pdf file and replace page 1 of the body.pdf with page 1 of the cover.pdf.
C. Save the new file as the android-cdd_x_x.pdf (where X_X is the number of the release, such as 5.1).

QUESTIONS?
==================
- For details on wkhtmltopdf, see http://wkhtmltopdf.org/usage/wkhtmltopdf.txt.
- all cdd html, css, and current pdf files are in docs/source.android.com/src/compatibility/release-number (currently 5.1).
- all cdd images are in docs/source.android.com/src/compatibility/images.
- all files currently in the lmp-dev-mr1 branch.