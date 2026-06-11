$src = 'C:\Users\lyd\.cursor\projects\c-Users-lyd-Desktop-ContentGenius\assets\c__Users_lyd_AppData_Roaming_Cursor_User_workspaceStorage_4d96fe6d2e70a56587028bd78f16b392_images_AI_____logo__-162d1b81-a8c7-4841-b444-df755d6e331b.png'
$dest = 'C:\Users\lyd\Desktop\ContentGenius\frontend\public\logo.png'
Add-Type -AssemblyName System.Drawing

function Get-GreenBounds($bmp) {
    $w = $bmp.Width; $h = $bmp.Height
    $minX = $w; $minY = $h; $maxX = 0; $maxY = 0
    for ($y = 0; $y -lt $h; $y++) {
        for ($x = 0; $x -lt $w; $x++) {
            $p = $bmp.GetPixel($x, $y)
            $isGreen = ($p.G -gt 95) -and (($p.G - $p.R) -gt 18) -and (($p.G - $p.B) -gt 10)
            if ($isGreen) {
                if ($x -lt $minX) { $minX = $x }
                if ($y -lt $minY) { $minY = $y }
                if ($x -gt $maxX) { $maxX = $x }
                if ($y -gt $maxY) { $maxY = $y }
            }
        }
    }
    return @{ X = $minX; Y = $minY; W = ($maxX - $minX + 1); H = ($maxY - $minY + 1) }
}

$img = [System.Drawing.Image]::FromFile($src)
$w = $img.Width; $h = $img.Height

# 主区域：去掉外框和底部水印
$rx = [int]($w * 0.16)
$ry = [int]($h * 0.24)
$rw = [int]($w * 0.68)
$rh = [int]($h * 0.50)

$stage = New-Object System.Drawing.Bitmap $rw, $rh
$g0 = [System.Drawing.Graphics]::FromImage($stage)
$g0.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
$srcRect = New-Object System.Drawing.Rectangle $rx, $ry, $rw, $rh
$dstRect = New-Object System.Drawing.Rectangle 0, 0, $rw, $rh
$g0.DrawImage($img, $dstRect, $srcRect, [System.Drawing.GraphicsUnit]::Pixel)
$g0.Dispose()
$img.Dispose()

$b = Get-GreenBounds $stage
$pad = [int]([Math]::Max($b.W, $b.H) * 0.16)
$side = [Math]::Max($b.W, $b.H) + 2 * $pad

$out = New-Object System.Drawing.Bitmap $side, $side
$g = [System.Drawing.Graphics]::FromImage($out)
$g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
$g.Clear([System.Drawing.Color]::FromArgb(255, 250, 250, 248))
$dx = [int](($side - $b.W) / 2)
$dy = [int](($side - $b.H) / 2)
$srcR = New-Object System.Drawing.Rectangle $b.X, $b.Y, $b.W, $b.H
$dstR = New-Object System.Drawing.Rectangle $dx, $dy, $b.W, $b.H
$g.DrawImage($stage, $dstR, $srcR, [System.Drawing.GraphicsUnit]::Pixel)
$out.Save($dest, [System.Drawing.Imaging.ImageFormat]::Png)
$g.Dispose(); $out.Dispose(); $stage.Dispose()
Write-Host "Saved $side x $side"
