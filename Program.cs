using Microsoft.Extensions.Configuration;
using DotNetMongoBoilerplate.Services;

// Build configuration
var configuration = new ConfigurationBuilder()
    .SetBasePath(Directory.GetCurrentDirectory())
    .AddJsonFile("appsettings.json", optional: true, reloadOnChange: true)
    .AddEnvironmentVariables()
    .Build();

var service = new SensorTimeSeriesService(configuration);
var simulator = new IotSimulator();

Console.WriteLine("=== .NET IoT + MongoDB Time Series ===\n");
PrintHelp();

var connected = false;
try
{
    await service.EnsureTimeSeriesCollectionAsync();
    connected = true;
}
catch (MongoDB.Driver.MongoConnectionException ex)
{
    Console.WriteLine($"MongoDB connection failed: {ex.Message}");
    Console.WriteLine("Ensure you have configured appsettings.json with your MongoDB Atlas connection string.\n");
}

while (connected)
{
    Console.Write("> ");
    var input = Console.ReadLine()?.Trim();
    if (string.IsNullOrEmpty(input)) continue;

    var parts = input.Split(' ', StringSplitOptions.RemoveEmptyEntries);
    var cmd = parts[0].ToLowerInvariant();
    var cmdArgs = parts.Skip(1).ToArray();

    try
    {
        switch (cmd)
        {
            case "init":
                await service.EnsureTimeSeriesCollectionAsync();
                Console.WriteLine("  Collection ready.\n");
                break;

            case "generate":
            case "gen":
                var count = cmdArgs.Length > 0 && int.TryParse(cmdArgs[0], out var c) ? c : 20;
                var interval = cmdArgs.Length > 1 && int.TryParse(cmdArgs[1], out var i) ? i : 30;
                var readings = simulator.GenerateTimeSeries(count: count, intervalSeconds: interval);
                await service.InsertManyAsync(readings);
                Console.WriteLine($"  Inserted {readings.Count} readings from {IotSimulator.DefaultSensors.Count} sensors.\n");
                break;

            case "read":
                var limit = cmdArgs.Length > 0 && int.TryParse(cmdArgs[0], out var l) ? l : 10;
                var docs = await service.GetRecentReadingsAsync(limit);
                Console.WriteLine($"  Latest {docs.Count} readings:\n");
                foreach (var d in docs)
                {
                    Console.WriteLine($"    {d.Timestamp:yyyy-MM-dd HH:mm:ss} | {d.Metadata.SensorId} | temp={d.Temperature:F1}°C, humidity={d.Humidity:F1}%");
                }
                Console.WriteLine();
                break;

            case "query":
            case "q":
                var queryType = cmdArgs.Length > 0 ? cmdArgs[0].ToLowerInvariant() : "all";
                await RunQueryAsync(service, queryType);
                break;

            case "stats":
                var total = await service.GetCountAsync();
                Console.WriteLine($"  Total documents: {total}\n");
                break;

            case "clear":
                await service.DropCollectionAsync();
                Console.WriteLine("  Collection dropped. Run 'init' to recreate.\n");
                break;

            case "help":
            case "?":
            case "h":
                PrintHelp();
                break;

            case "exit":
            case "quit":
                Console.WriteLine("Exiting.");
                return 0;

            default:
                Console.WriteLine($"  Unknown command: {cmd}. Type 'help' for commands.\n");
                break;
        }
    }
    catch (Exception ex)
    {
        Console.WriteLine($"  Error: {ex.Message}\n");
    }
}

return 0;

static void PrintHelp()
{
    Console.WriteLine("Commands:");
    Console.WriteLine("  init              Ensure time series collection exists");
    Console.WriteLine("  generate [count] [interval]   Generate sensor data (default: 20 batches, 30s interval)");
    Console.WriteLine("  read [limit]      Read recent readings (default: 10)");
    Console.WriteLine("  query [type]      Run aggregation: avg | time | stats | all");
    Console.WriteLine("  stats             Show document count");
    Console.WriteLine("  clear             Drop collection (reset)");
    Console.WriteLine("  help              Show this help");
    Console.WriteLine("  exit              Exit");
    Console.WriteLine();
}

static async Task RunQueryAsync(SensorTimeSeriesService service, string type)
{
    if (type == "avg" || type == "all")
    {
        Console.WriteLine("  Aggregation: Average per sensor");
        var bySensor = await service.GetAveragesBySensorAsync();
        foreach (var doc in bySensor)
        {
            Console.WriteLine($"    {doc["_id"]}: avg temp={doc["avgTemp"].ToDouble():F1}°C, avg humidity={doc["avgHumidity"].ToDouble():F1}%, count={doc["count"]}");
        }
        Console.WriteLine();
    }

    if (type == "time" || type == "all")
    {
        Console.WriteLine("  Aggregation: By time window (minute)");
        var byTime = await service.GetReadingsByTimeWindowAsync();
        foreach (var doc in byTime.Take(10))
        {
            Console.WriteLine($"    {doc["_id"]}: avg={doc["avgTemp"].ToDouble():F1}°C (min={doc["minTemp"].ToDouble():F1}, max={doc["maxTemp"].ToDouble():F1}), count={doc["count"]}");
        }
        if (byTime.Count > 10) Console.WriteLine($"    ... and {byTime.Count - 10} more");
        Console.WriteLine();
    }

    if (type == "stats" || type == "all")
    {
        Console.WriteLine("  Aggregation: Multi-facet statistics");
        var stats = await service.GetSensorStatisticsAsync();
        if (stats != null && stats.Contains("totalReadings"))
        {
            var totalArray = stats["totalReadings"].AsBsonArray;
            var total = totalArray.Count > 0 ? totalArray[0]["count"].ToInt64() : 0;
            Console.WriteLine($"    Total readings: {total}");
            if (stats.Contains("globalStats") && stats["globalStats"].AsBsonArray.Count > 0)
            {
                var g = stats["globalStats"].AsBsonArray[0].AsBsonDocument;
                Console.WriteLine($"    Global: avg temp={g["avgTemp"].ToDouble():F1}°C, avg humidity={g["avgHumidity"].ToDouble():F1}%");
            }
        }
        Console.WriteLine();
    }

    if (type != "avg" && type != "time" && type != "stats" && type != "all")
    {
        Console.WriteLine("  Unknown query type. Use: avg | time | stats | all");
    }
}
