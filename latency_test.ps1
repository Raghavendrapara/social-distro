# RAG Query Latency Test Script
$baseUrl = "http://localhost:8080"

Write-Host "=== RAG Query Latency Test ===" -ForegroundColor Cyan

# Step 1: Create Pod
Write-Host "`n[1/4] Creating test pod..." -ForegroundColor Yellow
$podResponse = Invoke-RestMethod -Uri "$baseUrl/pods" -Method Post -ContentType "application/json" -Body '{"name": "Latency Test", "ownerUserId": "perf-test"}'
$podId = $podResponse.podId
Write-Host "Pod created: $podId" -ForegroundColor Green

# Step 2: Add test data
Write-Host "`n[2/4] Adding test data..." -ForegroundColor Yellow
$testData = @(
    "Raghav is a Senior Software Engineer at Google DeepMind working on Agentic AI and large language models.",
    "The Social Distro project demonstrates scalable hexagonal architecture with Spring Boot and Kafka.",
    "Virtual threads in Java 21 enable efficient handling of I/O-bound operations without blocking.",
    "PostgreSQL with pgvector extension provides HNSW indexes for fast vector similarity search.",
    "Redis caching significantly reduces latency for frequently accessed embeddings and LLM responses."
)

foreach ($content in $testData) {
    $body = @{content = $content} | ConvertTo-Json
    Invoke-RestMethod -Uri "$baseUrl/pods/$podId/data" -Method Post -ContentType "application/json" -Body $body | Out-Null
    Write-Host "  Added: $($content.Substring(0,50))..." -ForegroundColor Gray
}
Write-Host "Data added successfully" -ForegroundColor Green

# Step 3: Trigger indexing and wait
Write-Host "`n[3/4] Triggering indexing (embedding generation)..." -ForegroundColor Yellow
$indexResponse = Invoke-RestMethod -Uri "$baseUrl/indexing/pods/$podId" -Method Post
$jobId = $indexResponse.jobId
Write-Host "Job ID: $jobId" -ForegroundColor Gray

# Wait for indexing to complete
$maxAttempts = 30
$attempt = 0
do {
    Start-Sleep -Seconds 2
    $attempt++
    try {
        $jobStatus = Invoke-RestMethod -Uri "$baseUrl/indexing/jobs/$jobId" -Method Get
        Write-Host "  Status: $($jobStatus.status)" -ForegroundColor Gray
    } catch {
        Write-Host "  Waiting for job..." -ForegroundColor Gray
    }
} while ($jobStatus.status -ne "COMPLETED" -and $attempt -lt $maxAttempts)

if ($jobStatus.status -eq "COMPLETED") {
    Write-Host "Indexing completed!" -ForegroundColor Green
} else {
    Write-Host "Indexing may still be in progress..." -ForegroundColor Yellow
}

# Wait a bit more for item-level indexing
Write-Host "Waiting for embedding generation..." -ForegroundColor Gray
Start-Sleep -Seconds 10

# Step 4: Run RAG queries and measure latency
Write-Host "`n[4/4] Running RAG queries and measuring latency..." -ForegroundColor Yellow
$queries = @(
    "Where does Raghav work?",
    "What is Social Distro?",
    "What are virtual threads?",
    "How does vector search work?",
    "What is Redis used for?"
)

$latencies = @()
$results = @()

foreach ($question in $queries) {
    $body = @{question = $question} | ConvertTo-Json
    
    $stopwatch = [System.Diagnostics.Stopwatch]::StartNew()
    try {
        $response = Invoke-RestMethod -Uri "$baseUrl/pods/$podId/query" -Method Post -ContentType "application/json" -Body $body
        $stopwatch.Stop()
        $latencyMs = $stopwatch.ElapsedMilliseconds
        $latencies += $latencyMs
        
        Write-Host "`n  Q: $question" -ForegroundColor White
        Write-Host "  A: $($response.answer.Substring(0, [Math]::Min(100, $response.answer.Length)))..." -ForegroundColor Gray
        Write-Host "  Latency: $latencyMs ms" -ForegroundColor $(if ($latencyMs -lt 100) { "Green" } elseif ($latencyMs -lt 500) { "Yellow" } else { "Red" })
        
        $results += [PSCustomObject]@{
            Query = $question
            LatencyMs = $latencyMs
            Answer = $response.answer.Substring(0, [Math]::Min(50, $response.answer.Length))
        }
    } catch {
        $stopwatch.Stop()
        Write-Host "`n  Q: $question" -ForegroundColor White
        Write-Host "  Error: $_" -ForegroundColor Red
    }
}

# Summary
Write-Host "`n=== LATENCY SUMMARY ===" -ForegroundColor Cyan
if ($latencies.Count -gt 0) {
    $avgLatency = ($latencies | Measure-Object -Average).Average
    $minLatency = ($latencies | Measure-Object -Minimum).Minimum
    $maxLatency = ($latencies | Measure-Object -Maximum).Maximum
    $p50 = ($latencies | Sort-Object)[([Math]::Floor($latencies.Count * 0.5))]
    
    Write-Host "Queries run: $($latencies.Count)" -ForegroundColor White
    Write-Host "Min latency: $minLatency ms" -ForegroundColor Green
    Write-Host "Max latency: $maxLatency ms" -ForegroundColor Yellow
    Write-Host "Avg latency: $([Math]::Round($avgLatency, 2)) ms" -ForegroundColor Cyan
    Write-Host "P50 latency: $p50 ms" -ForegroundColor Cyan
    
    # Verdict
    Write-Host "`n=== RESUME VERDICT ===" -ForegroundColor Magenta
    if ($avgLatency -lt 100) {
        Write-Host "✅ 'sub-100ms RAG queries' is ACCURATE!" -ForegroundColor Green
    } elseif ($avgLatency -lt 500) {
        Write-Host "⚠️ Suggest using 'sub-500ms RAG queries' or 'low-latency'" -ForegroundColor Yellow
    } else {
        Write-Host "❌ Latency is high - use 'optimized RAG queries' instead" -ForegroundColor Red
    }
}
