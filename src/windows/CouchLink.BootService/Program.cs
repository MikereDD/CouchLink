using CouchLink.BootService;
using Microsoft.Extensions.Logging;

if (args.Contains("--hid-smoke-test", StringComparer.OrdinalIgnoreCase))
{
    using ILoggerFactory loggerFactory = LoggerFactory.Create(logging =>
    {
        logging.AddSimpleConsole(options =>
        {
            options.SingleLine = true;
            options.TimestampFormat = "HH:mm:ss ";
        });

        logging.SetMinimumLevel(LogLevel.Information);
    });

    ILogger logger = loggerFactory.CreateLogger("CouchLink.HidSmokeTest");

    using var hid = new VirtualHidBridge(logger);

    if (!hid.IsAvailable)
    {
        Console.Error.WriteLine(
            "CouchLink Virtual HID could not be opened. Run this test as Administrator.");
        Environment.ExitCode = 1;
        return;
    }

    Console.WriteLine("Virtual HID opened successfully.");
    Console.WriteLine("Moving the pointer 2 pixels to the right.");

    hid.MoveRelative(2, 0);
    hid.ReleaseAll();

    Console.WriteLine("Smoke test completed.");
    return;
}

HostApplicationBuilder builder = Host.CreateApplicationBuilder(args);
builder.Services.AddWindowsService(options =>
    options.ServiceName = BootWorker.ServiceName);
builder.Services.AddHostedService<BootWorker>();

await builder.Build().RunAsync();