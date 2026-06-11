$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$jsonPath = Join-Path $scriptDir 'resume-content.json'
$outPath = 'C:\Users\lyd\Desktop\Resume_LiuYadong.docx'
$tempDir = Join-Path $env:TEMP ('resume_docx_' + (Get-Random))

New-Item -ItemType Directory -Path $tempDir -Force | Out-Null
New-Item -ItemType Directory -Path (Join-Path $tempDir '_rels') -Force | Out-Null
New-Item -ItemType Directory -Path (Join-Path $tempDir 'word\_rels') -Force | Out-Null

function Escape-Xml([string]$s) {
    return [System.Security.SecurityElement]::Escape($s)
}

function Para([string]$text, [bool]$bold, [int]$size, [string]$align) {
    $b = if ($bold) { '<w:b/>' } else { '' }
    $escaped = Escape-Xml $text
    return @"
<w:p>
  <w:pPr><w:jc w:val="$align"/></w:pPr>
  <w:r>
    <w:rPr>$b<w:sz w:val="$size"/><w:szCs w:val="$size"/><w:rFonts w:eastAsia="Microsoft YaHei"/></w:rPr>
    <w:t xml:space="preserve">$escaped</w:t>
  </w:r>
</w:p>
"@
}

$utf8 = New-Object System.Text.UTF8Encoding $false
$json = [System.IO.File]::ReadAllText($jsonPath, $utf8)
$lines = $json | ConvertFrom-Json

$paras = New-Object System.Collections.Generic.List[string]
foreach ($item in $lines) {
    if ($item.e) {
        $paras.Add('<w:p/>')
        continue
    }
    $text = $item.t
    if ($item.bullet) { $text = [char]0x2022 + ' ' + $text }
    $bold = [bool]($item.b)
    $size = if ($item.s) { [int]$item.s } else { 21 }
    $align = if ($item.a) { $item.a } else { 'left' }
    $paras.Add((Para $text $bold $size $align))
}

$body = $paras -join "`n"
$documentXml = @"
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
  <w:body>
$body
    <w:sectPr>
      <w:pgSz w:w="11906" w:h="16838"/>
      <w:pgMar w:top="1134" w:right="1134" w:bottom="1134" w:left="1134"/>
    </w:sectPr>
  </w:body>
</w:document>
"@

$contentTypes = '<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"><Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/><Default Extension="xml" ContentType="application/xml"/><Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/></Types>'
$rels = '<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/></Relationships>'
$docRels = '<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"></Relationships>'

[System.IO.File]::WriteAllText((Join-Path $tempDir '[Content_Types].xml'), $contentTypes, $utf8)
[System.IO.File]::WriteAllText((Join-Path $tempDir '_rels\.rels'), $rels, $utf8)
[System.IO.File]::WriteAllText((Join-Path $tempDir 'word\document.xml'), $documentXml, $utf8)
[System.IO.File]::WriteAllText((Join-Path $tempDir 'word\_rels\document.xml.rels'), $docRels, $utf8)

$zipPath = $outPath + '.zip'
if (Test-Path $zipPath) { Remove-Item $zipPath -Force }
if (Test-Path $outPath) { Remove-Item $outPath -Force }
Compress-Archive -Path (Join-Path $tempDir '*') -DestinationPath $zipPath -Force
Move-Item $zipPath $outPath -Force
Remove-Item $tempDir -Recurse -Force
Write-Host "Created: $outPath"
